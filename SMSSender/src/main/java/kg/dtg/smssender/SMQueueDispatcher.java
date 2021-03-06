package kg.dtg.smssender;

import com.adenki.smpp.Address;
import com.adenki.smpp.SessionImpl;
import com.adenki.smpp.encoding.*;
import com.adenki.smpp.event.*;
import com.adenki.smpp.message.*;
import com.adenki.smpp.message.tlv.Tag;
import com.adenki.smpp.net.TcpLink;
import com.adenki.smpp.version.SMPPVersion;
import kg.dtg.smssender.Operations.*;
import kg.dtg.smssender.events.*;
import kg.dtg.smssender.statistic.IncrementalCounterToken;
import kg.dtg.smssender.statistic.MinMaxCounterToken;
import kg.dtg.smssender.statistic.SoftTime;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 4/9/11
 * Time: 8:11 PM
 */
public final class SMQueueDispatcher implements SessionObserver, Runnable {
  private static final String THREAD_NAME = "Short message queue dispatcher";
  private static final Logger LOGGER = Logger.getLogger(SMQueueDispatcher.class);

  private static final int OCTET_UNSPECIFIED_CODING = 8;
  private static final String DELIVERY_SM_DATE_FORMAT = "yyMMddHHmm";

  private static final Pattern DELIVERY_SM_PATTERN = Pattern.compile("^id:([0-9A-Fa-f]+)(.+)done date:(\\d+)(.+)stat:(\\w+)($|(.+)$)");

  private static final int ID_GROUP = 1;
  private static final int DONE_DATE_GROUP = 3;
  private static final int MESSAGE_STATE_GROUP = 5;

  private static final int MESSAGE_DELIVERED_STATE = 2;
  private static final int MESSAGE_EXPIRED_STATE = 3;
  private static final int MESSAGE_DELETED_STATE = 4;
  private static final int MESSAGE_UNDELIVERED_STATE = 5;
  private static final int MESSAGE_ACCEPTED_STATE = 6;
  private static final int MESSAGE_UNKNOWN_STATE = 7;
  private static final int MESSAGE_REJECTED_STATE = 8;

  private static final String MESSAGE_DELIVERED_STATE_STRING = "DELIVRD";
  private static final String MESSAGE_EXPIRED_STATE_STRING = "EXPIRED";
  private static final String MESSAGE_DELETED_STATE_STRING = "DELETED";
  private static final String MESSAGE_UNDELIVERED_STATE_STRING = "UNDELIV";
  private static final String MESSAGE_ACCEPTED_STATE_STRING = "ACCEPTD";
  private static final String MESSAGE_UNKNOWN_STATE_STRING = "UNKNOWN";
  private static final String MESSAGE_REJECTED_STATE_STRING = "REJECTD";

  private static final String UNKNOWN_STRING = "<unknown>";

  private final String latinAlphabetPattern;
  public final AlphabetEncoding latinEncoding;
  public final Integer latinDataCoding;

  public final AlphabetEncoding nonLatinEncoding;
  public final Integer nonLatinDataCoding;

  private final boolean alwaysMessagePayload;
  private final int registeredDelivery;
  private final boolean useDataSm;

  private final BlockingQueue<Operation> pendingOperations = new LinkedBlockingQueue<>();

  private final String sourceHost;
  private final Integer sourcePort;
  private final String destinationHost;
  private final int destinationPort;
  private final String systemId;
  private final String password;
  private final String serviceType;
  private final Byte ussdServiceOpValue;
  private final int resumeQueueSize;
  private final int pauseQueueSize;
  private final int keepAlive;
  private final int sendInterval;

  private final ConcurrentMap<Long, Operation> pendingResponses = new ConcurrentHashMap<>();

  private final MinMaxCounterToken queueSizeCounter;
  private final IncrementalCounterToken submitOkCounter;
  private final IncrementalCounterToken submitFailedCounter;
  private final IncrementalCounterToken submitRespOkCounter;
  private final IncrementalCounterToken submitRespFailedCounter;

  private final IncrementalCounterToken cancelOkCounter;
  private final IncrementalCounterToken cancelFailedCounter;
  private final IncrementalCounterToken cancelRespOkCounter;
  private final IncrementalCounterToken cancelRespFailedCounter;

  private final IncrementalCounterToken deliveredMessagesCounter;
  private final IncrementalCounterToken deliverSMMessagesCounter;
  private final IncrementalCounterToken receivedMessagesCounter;
  private final MinMaxCounterToken submitSMTimeCounter;
  private final MinMaxCounterToken cancelSMTimeCounter;

  private com.adenki.smpp.Session smppSession;
  private final Object sessionSyncObject = new Object();

  private boolean queryDispatchersPaused = false;
  private SMDispatcherState state = SMDispatcherState.NOT_CONNECTED;

  private static SMQueueDispatcher SMQueueDispatcher;

  public static void initialize(Properties properties) throws Exception {
    SMQueueDispatcher = new SMQueueDispatcher(properties);
  }

  public static void emit(final Operation operation) throws InterruptedException {
    SMQueueDispatcher.pendingOperations.put(operation);
  }

  public static boolean canEmit() {
    if (SMQueueDispatcher.pendingOperations.size() > SMQueueDispatcher.pauseQueueSize && !SMQueueDispatcher.queryDispatchersPaused) {
      QueryDispatcher.pause();
      SMQueueDispatcher.queryDispatchersPaused = true;
      return false;
    }

    return true;
  }

  private SMQueueDispatcher(final Properties properties) throws Exception {
    this.sourceHost = properties.getProperty("smsc.src_host");
    if (properties.getProperty("smsc.src_port") != null)
      this.sourcePort = Integer.parseInt(properties.getProperty("smsc.src_port"));
    else
      this.sourcePort = null;

    this.destinationHost = properties.getProperty("smsc.dst_host");
    this.destinationPort = Integer.parseInt(properties.getProperty("smsc.dst_port"));

    this.systemId = properties.getProperty("bind.systemId");
    this.password = properties.getProperty("bind.password");
    this.serviceType = properties.getProperty("bind.serviceType");
    this.ussdServiceOpValue = Byte.parseByte(properties.getProperty("ussd_service_op.value"));

    this.pauseQueueSize = Integer.parseInt(properties.getProperty("smssender.queryDispatcher.pauseQueueSize"));
    this.resumeQueueSize = Integer.parseInt(properties.getProperty("smssender.resumeQueueSize"));

    this.keepAlive = Integer.parseInt(properties.getProperty("smssender.keepAlive"));
    this.sendInterval = 1000 / Integer.parseInt(properties.getProperty("smssender.maxMessagesPerSecond"));

    this.latinAlphabetPattern = properties.getProperty("smssender.latin_message.pattern");
    this.latinEncoding = GetEncoding(properties.getProperty("smssender.latin_message.encoding"));

    if (properties.getProperty("smssender.latin_message.data_coding") != null)
      this.latinDataCoding = Integer.parseInt(properties.getProperty("smssender.latin_message.data_coding"));
    else
      this.latinDataCoding = null;

    this.nonLatinEncoding = GetEncoding(properties.getProperty("smssender.non_latin_message.encoding"));

    if (properties.getProperty("smssender.non_latin_message.data_coding") != null)
      this.nonLatinDataCoding = Integer.parseInt(properties.getProperty("smssender.non_latin_message.data_coding"));
    else
      this.nonLatinDataCoding = null;

    this.alwaysMessagePayload = Boolean.parseBoolean(properties.getProperty("smssender.always_message_payload"));
    this.registeredDelivery = Integer.parseInt(properties.getProperty("smssender.registered_delivery"));
    this.useDataSm = Boolean.parseBoolean(properties.getProperty("smssender.use_data_sm"));

    queueSizeCounter = new MinMaxCounterToken("SM Queue dispatcher: Queue size", "count");
    submitOkCounter = new IncrementalCounterToken("SM Queue dispatcher: Submit SM - OK", "count");
    submitFailedCounter = new IncrementalCounterToken("SM Queue dispatcher: Submit SM - Failed", "count");
    submitRespOkCounter = new IncrementalCounterToken("SM Queue dispatcher: Submit SM Resp - OK", "count");
    submitRespFailedCounter = new IncrementalCounterToken("SM Queue dispatcher: Submit SM Resp - Failed", "count");

    cancelOkCounter = new IncrementalCounterToken("SM Queue dispatcher: Cancel SM - OK", "count");
    cancelFailedCounter = new IncrementalCounterToken("SM Queue dispatcher: Cancel SM - Failed", "count");
    cancelRespOkCounter = new IncrementalCounterToken("SM Queue dispatcher: Cancel SM Resp - OK", "count");
    cancelRespFailedCounter = new IncrementalCounterToken("SM Queue dispatcher: Cancel SM Resp - Failed", "count");

    deliverSMMessagesCounter = new IncrementalCounterToken("SM Queue dispatcher: Deliver SM messages", "count");
    deliveredMessagesCounter = new IncrementalCounterToken("SM Queue dispatcher: Deliver SM messages (Delivered Status)", "count");
    receivedMessagesCounter = new IncrementalCounterToken("SM Queue dispatcher: Received messages", "count");
    submitSMTimeCounter = new MinMaxCounterToken("SM Queue dispatcher: Submit SM time", "milliseconds");
    cancelSMTimeCounter = new MinMaxCounterToken("SM Queue dispatcher: Cancel SM time", "milliseconds");

    new Thread(this).start();
  }

  public static SMDispatcherState getState() {
    return SMQueueDispatcher.state;
  }

  private void connect() throws Exception {
    LOGGER.info("Connecting to sms center...");

    if (sourceHost != null && sourcePort != null) {
      if (smppSession != null && smppSession.getSmscLink() != null) {
        try {
          smppSession.getSmscLink().disconnect();
        } catch (Throwable ignored) {
        }
      }

      final Socket socket = new Socket(destinationHost, destinationPort, InetAddress.getByName(sourceHost), sourcePort);
      smppSession = new SessionImpl(new TcpLink(socket));
    } else {
      smppSession = new SessionImpl(destinationHost, destinationPort);
    }

    smppSession.addObserver(this);

    final BindTransceiver bindTransceiver = new BindTransceiver();
    bindTransceiver.setVersion(SMPPVersion.VERSION_3_4);
    bindTransceiver.setSystemId(systemId);
    bindTransceiver.setPassword(password);
    bindTransceiver.setSystemType(serviceType);

    smppSession.bind(bindTransceiver);

    state = SMDispatcherState.WAIT_CONNECTION;

    LOGGER.info("Waiting bind response...");
  }

  @Override
  public final void run() {
    try {
      int c = 0;

      //noinspection InfiniteLoopStatement
      for (; ; ) {
        if (state != SMDispatcherState.CONNECTED) {
          if (state == SMDispatcherState.NOT_CONNECTED) {
            try {
              connect();
            } catch (Exception e) {
              LOGGER.warn("Cannot connect to sms center", e);
            }
          }

          Thread.sleep(5000);
          continue;
        }

        if (queryDispatchersPaused && pendingOperations.size() <= resumeQueueSize) {
          QueryDispatcher.resume();
          queryDispatchersPaused = false;
        }

        c = ++c % 10;

        if (c == 9) {
          queueSizeCounter.setValue(pendingOperations.size());
        }

        final Operation currentOperation = pendingOperations.poll(keepAlive, TimeUnit.MILLISECONDS);

        if (currentOperation == null) {
          try {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Enquire link");
            }

            synchronized (sessionSyncObject) {
              smppSession.send(new EnquireLink());
            }
          } catch (IOException ignored) {
          }

          continue;
        }

        if (LOGGER.isDebugEnabled())
          LOGGER.info(String.format("<SMQueue> Operation#%s received", currentOperation.getUid()));

        final long startTime = SoftTime.getTimestamp();
        if (currentOperation instanceof SubmitOperation) {
          if (this.useDataSm)
            submitData(currentOperation);
          else
            submitMessage(currentOperation);

          submitSMTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
        } else if (currentOperation instanceof ReplaceShortMessageOperation) {
          switch (currentOperation.getState()) {
            case OperationState.SUBMITTED:
              cancelMessage(currentOperation);

              cancelSMTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
              break;

            case OperationState.SCHEDULED:
            case OperationState.CANCELLED_TO_REPLACE:
              if (this.useDataSm)
                submitData(currentOperation);
              else
                submitMessage(currentOperation);

              submitSMTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
              break;
          }
        }

        Thread.sleep(sendInterval);
      }
    } catch (InterruptedException ignored) {
    }
  }

  @Override
  public final void packetReceived(final com.adenki.smpp.Session source, final SMPPPacket packet) {
    if (LOGGER.isDebugEnabled())
      LOGGER.debug(String.format("Received:\n%s", packet));

    switch (packet.getCommandId()) {
      case CommandId.BIND_TRANSCEIVER_RESP:

        if (packet.getCommandStatus() == 0) {
          LOGGER.info("Successfully connected to sms center");
          state = SMDispatcherState.CONNECTED;
        } else {
          state = SMDispatcherState.NOT_CONNECTED;
          LOGGER.warn(String.format("Cannot connect to sms center, command status: %s", packet.getCommandStatus()));
        }
        break;

      case CommandId.SUBMIT_SM_RESP:
        submitSMResponse((SubmitSMResp) packet);
        break;

      case CommandId.DATA_SM_RESP:
        dataSMResponse((DataSMResp) packet);
        break;

      case CommandId.CANCEL_SM_RESP:
        cancelSMResponse((CancelSMResp) packet);
        break;

      case CommandId.DELIVER_SM:
        deliverSM((DeliverSM) packet);
        break;

      case CommandId.ENQUIRE_LINK:
        try {
          synchronized (sessionSyncObject) {
            smppSession.send(new EnquireLinkResp(packet));
          }
        } catch (IOException e) {
          LOGGER.warn("Cannot send enquire link response", e);
        }
        break;
    }
  }

  @Override
  public final void update(final com.adenki.smpp.Session source, final SMPPEvent event) {
    if (event instanceof ReceiverStartEvent) {
      LOGGER.info("Receiver started...");
      state = SMDispatcherState.CONNECTED;
    } else if (event instanceof ReceiverExceptionEvent) {
      ReceiverExceptionEvent receiverExceptionEvent = (ReceiverExceptionEvent) event;

      LOGGER.warn(String.format("Receiver exited (state %s)", receiverExceptionEvent.getState()), receiverExceptionEvent.getException());
      state = SMDispatcherState.NOT_CONNECTED;
    } else if (event instanceof ReceiverExitEvent) {
      ReceiverExitEvent exitEvent = (ReceiverExitEvent) event;

      LOGGER.warn(String.format("Receiver exited (state %s)", exitEvent.getState()), exitEvent.getException());

      state = SMDispatcherState.NOT_CONNECTED;
    }
  }

  private void submitMessage(final Operation operation) throws InterruptedException {
    if (LOGGER.isDebugEnabled())
      LOGGER.debug(String.format("Operation %s - submit message", operation.getUid()));

    try {
      EventDispatcher.emit(new SubmittingEvent(operation));

      final Address sourceAddress = new Address(operation.getSourceTon(), operation.getSourceNpi(), operation.getSourceNumber());
      final Address destinationAddress = new Address(operation.getDestinationTon(), operation.getDestinationNpi(), operation.getDestinationNumber());

      if (LOGGER.isDebugEnabled())
        LOGGER.debug(String.format("Submitting message from %s to %s", sourceAddress, destinationAddress));

      final SubmitSM submitSM = new SubmitSM();
      submitSM.setServiceType(operation.getServiceType());
      submitSM.setSource(sourceAddress);
      submitSM.setDestination(destinationAddress);
      submitSM.setRegistered(registeredDelivery);

      final String message;
      if (operation instanceof SubmitOperation) {
        message = ((SubmitOperation) operation).getMessage();
      } else {
        message = ((ReplaceShortMessageOperation) operation).getMessage();
      }

      int dataCoding;

      final AlphabetEncoding encoding;
      if (message.matches(latinAlphabetPattern)) {
        encoding = latinEncoding;

        if (latinDataCoding != null)
          dataCoding = latinDataCoding;
        else
          dataCoding = encoding.getDataCoding();
      } else {
        encoding = nonLatinEncoding;

        if (nonLatinDataCoding != null)
          dataCoding = nonLatinDataCoding;
        else
          dataCoding = encoding.getDataCoding();
      }

      submitSM.setDataCoding(dataCoding);

      final byte[] encodedMessage = encoding.encode(message);

      if (operation instanceof SubmitUSSDOperation) {
        submitSM.setTLV(Tag.USSD_SERVICE_OP, new byte[]{ussdServiceOpValue});
        submitSM.setMessage(encodedMessage);
      } else {
        if (alwaysMessagePayload) {
          submitSM.setTLV(Tag.MESSAGE_PAYLOAD, encodedMessage);
        } else {
          if (encoding == latinEncoding && encodedMessage.length < 160)
            submitSM.setMessage(encodedMessage);
          else if (encoding == nonLatinEncoding && encodedMessage.length < 70)
            submitSM.setMessage(encodedMessage);
          else
            submitSM.setTLV(Tag.MESSAGE_PAYLOAD, encodedMessage);
        }
      }

      synchronized (sessionSyncObject) {
        smppSession.send(submitSM);
      }

      if (LOGGER.isDebugEnabled())
        LOGGER.debug(String.format("Sending message %s, %s", message, submitSM));

      final long sequenceNum = submitSM.getSequenceNum();
      pendingResponses.put(sequenceNum, operation);

      try {
        submitOkCounter.incrementValue();
      } catch (InterruptedException ignored) {
      }
    } catch (Exception e) {
      try {
        submitFailedCounter.incrementValue();
      } catch (InterruptedException ignored) {
      }

      LOGGER.warn("Cannot send message", e);
      state = SMDispatcherState.NOT_CONNECTED;
    }
  }

  private void submitSMResponse(final SubmitSMResp submitSMResp) {
    final long sequenceNum = submitSMResp.getSequenceNum();
    final Operation operation = pendingResponses.remove(sequenceNum);

    LOGGER.info(String.format("Received submitSMResponse for message (sequence = %d, status = %d)", sequenceNum, submitSMResp.getCommandStatus()));
    try {
      final int messageId = submitSMResp.getMessageId() != null ? Integer.parseInt(submitSMResp.getMessageId(), 16) : -1;

      EventDispatcher.emit(new SubmittedEvent(operation, messageId, submitSMResp.getCommandStatus()));

      if (submitSMResp.getCommandStatus() != 0) {
        LOGGER.warn(String.format("Cannot submit message (command status: %s, messageId: %s)", submitSMResp.getCommandStatus(), messageId));
        try {
          submitRespFailedCounter.incrementValue();
        } catch (InterruptedException ignored) {
        }

        return;
      }

      try {
        submitRespOkCounter.incrementValue();
      } catch (InterruptedException ignored) {
      }
    } catch (Exception exception) {
      try {
        submitRespFailedCounter.incrementValue();
      } catch (InterruptedException ignored) {
      }

      LOGGER.error("Cannot dispatch submitSMResponse", exception);
    }
  }

  private void submitData(final Operation operation) throws InterruptedException {
    if (LOGGER.isDebugEnabled())
      LOGGER.debug(String.format("Operation %s - submit message", operation.getUid()));

    try {
      EventDispatcher.emit(new SubmittingEvent(operation));

      final Address sourceAddress = new Address(operation.getSourceTon(), operation.getSourceNpi(), operation.getSourceNumber());
      final Address destinationAddress = new Address(operation.getDestinationTon(), operation.getDestinationNpi(), operation.getDestinationNumber());

      if (LOGGER.isDebugEnabled())
        LOGGER.debug(String.format("Submitting message from %s to %s", sourceAddress, destinationAddress));

      final DataSM dataSM = new DataSM();
      dataSM.setServiceType(operation.getServiceType());
      dataSM.setSource(sourceAddress);
      dataSM.setDestination(destinationAddress);
      dataSM.setRegistered(registeredDelivery);

      final String message;
      if (operation instanceof SubmitOperation) {
        message = ((SubmitOperation) operation).getMessage();
      } else {
        message = ((ReplaceShortMessageOperation) operation).getMessage();
      }

      int dataCoding;

      final AlphabetEncoding encoding;
      if (message.matches(latinAlphabetPattern)) {
        encoding = latinEncoding;

        if (latinDataCoding != null)
          dataCoding = latinDataCoding;
        else
          dataCoding = encoding.getDataCoding();
      } else {
        encoding = nonLatinEncoding;

        if (nonLatinDataCoding != null)
          dataCoding = nonLatinDataCoding;
        else
          dataCoding = encoding.getDataCoding();
      }

      dataSM.setDataCoding(dataCoding);

      final byte[] encodedMessage = encoding.encode(message);

      if (operation instanceof SubmitUSSDOperation) {
        dataSM.setTLV(Tag.USSD_SERVICE_OP, new byte[]{ussdServiceOpValue});
      }

      dataSM.setTLV(Tag.MESSAGE_PAYLOAD, encodedMessage);

      synchronized (sessionSyncObject) {
        smppSession.send(dataSM);
      }

      if (LOGGER.isDebugEnabled())
        LOGGER.debug(String.format("Sending message %s, %s", message, dataSM));

      final long sequenceNum = dataSM.getSequenceNum();
      pendingResponses.put(sequenceNum, operation);

      try {
        submitOkCounter.incrementValue();
      } catch (InterruptedException ignored) {
      }
    } catch (Exception e) {
      LOGGER.warn("Cannot send message", e);
      state = SMDispatcherState.NOT_CONNECTED;

      try {
        submitFailedCounter.incrementValue();
      } catch (InterruptedException ignored) {
      }
    }
  }

  private void dataSMResponse(final DataSMResp dataSMResp) {
    final long sequenceNum = dataSMResp.getSequenceNum();
    final Operation operation = pendingResponses.remove(sequenceNum);

    LOGGER.info(String.format("Received dataSMResponse for message (sequence = %d, status = %d)", sequenceNum, dataSMResp.getCommandStatus()));
    try {
      final int messageId = dataSMResp.getMessageId() != null ? Integer.parseInt(dataSMResp.getMessageId(), 16) : -1;

      EventDispatcher.emit(new SubmittedEvent(operation, messageId, dataSMResp.getCommandStatus()));

      if (dataSMResp.getCommandStatus() != 0) {
        LOGGER.warn(String.format("Cannot submit message (command status: %s, messageId: %s)", dataSMResp.getCommandStatus(), messageId));
        return;
      }

      try {
        submitRespOkCounter.incrementValue();
      } catch (InterruptedException ignored) {
      }
    } catch (Exception exception) {
      try {
        submitRespFailedCounter.incrementValue();
      } catch (InterruptedException ignored) {
      }

      LOGGER.error("Cannot dispatch dataSMResponse", exception);
    }
  }

  private void cancelMessage(final Operation operation) {
    if (LOGGER.isDebugEnabled())
      LOGGER.debug(String.format("Operation %s - cancel message", operation.getUid()));

    try {
      int messageId;
      if (operation instanceof CancelShortMessageOperation) {
        EventDispatcher.emit(new CancellingEvent((CancelShortMessageOperation) operation));
        messageId = ((CancelShortMessageOperation) operation).getMessageId();
      } else {
        EventDispatcher.emit(new CancellingToReplaceEvent((ReplaceShortMessageOperation) operation));
        messageId = ((ReplaceShortMessageOperation) operation).getMessageId();
      }

      final Address sourceAddress = new Address(operation.getSourceTon(), operation.getSourceNpi(), operation.getSourceNumber());
      final Address destinationAddress = new Address(operation.getDestinationTon(), operation.getDestinationNpi(), operation.getDestinationNumber());

      final CancelSM cancelSM = new CancelSM();
      cancelSM.setServiceType(operation.getServiceType());
      cancelSM.setSource(sourceAddress);
      cancelSM.setDestination(destinationAddress);
      cancelSM.setMessageId(String.format("%08X", messageId));

      synchronized (sessionSyncObject) {
        smppSession.send(cancelSM);
      }

      if (LOGGER.isDebugEnabled())
        LOGGER.debug(String.format("Sending cancel message %s-%s, %s", operation.getSourceNumber(), operation.getDestinationNumber(), cancelSM));

      final long sequenceNum = cancelSM.getSequenceNum();
      pendingResponses.put(sequenceNum, operation);

      try {
        cancelOkCounter.incrementValue();
      } catch (InterruptedException ignored) {
      }
    } catch (Exception e) {
      LOGGER.warn("Cannot send message", e);
      state = SMDispatcherState.NOT_CONNECTED;

      try {
        cancelFailedCounter.incrementValue();
      } catch (InterruptedException ignored) {
      }
    }
  }

  private void cancelSMResponse(final CancelSMResp cancelSMResp) {
    final long sequenceNum = cancelSMResp.getSequenceNum();
    final Operation operation = pendingResponses.remove(sequenceNum);

    final int commandStatus = cancelSMResp.getCommandStatus();

    try {
      final int messageId;
      if (operation instanceof CancelShortMessageOperation) {
        messageId = ((CancelShortMessageOperation) operation).getMessageId();
        EventDispatcher.emit(new CancelledEvent((CancelShortMessageOperation) operation, commandStatus));
      } else {
        messageId = ((ReplaceShortMessageOperation) operation).getMessageId();
        EventDispatcher.emit(new CancelledToReplaceEvent((ReplaceShortMessageOperation) operation, commandStatus));
      }

      LOGGER.info(String.format("Received cancelSMResponse for message %d with %d status", messageId, commandStatus));

      if (commandStatus != 0) {
        try {
          cancelRespFailedCounter.incrementValue();
        } catch (InterruptedException ignored) {
        }

        LOGGER.warn(String.format("Cannot cancel message (command status: %s, messageId: %s)", commandStatus, messageId));
        return;
      }

      try {
        cancelRespOkCounter.incrementValue();
      } catch (InterruptedException ignored) {
      }
    } catch (Exception exception) {
      try {
        cancelRespFailedCounter.incrementValue();
      } catch (InterruptedException ignored) {
      }

      LOGGER.error("Cannot dispatch cancelSMResponse", exception);
    }
  }

  private void deliverSM(final DeliverSM deliverSM) {
    try {
      synchronized (sessionSyncObject) {
        smppSession.send(new DeliverSMResp(deliverSM));
      }
    } catch (IOException e) {
      LOGGER.warn("Cannot response DELIVER_SM_RESP", e);
    }

    final String message;
    final byte[] rawMessage = deliverSM.getMessage();
    if (deliverSM.getDataCoding() == OCTET_UNSPECIFIED_CODING) {
      message = nonLatinEncoding.decode(rawMessage);
    } else {
      message = latinEncoding.decode(rawMessage);
    }

    final int messageType;
    Integer messageId = null;
    Integer messageState = null;
    Timestamp timestamp = null;
    if (deliverSM.getTLVTable().containsKey(Tag.USSD_SERVICE_OP)) {
      messageType = MessageReceivedEvent.USSD_MESSAGE_TYPE;

      final String messageIdString = deliverSM.getTLVTable().getString(Tag.RECEIPTED_MESSAGE_ID);
      if (messageIdString != null)
        messageId = Integer.parseInt(messageIdString, 16);

      messageState = deliverSM.getTLVTable().getInt(Tag.MESSAGE_STATE);
      timestamp = new Timestamp(System.currentTimeMillis());

      if (LOGGER.isDebugEnabled())
        LOGGER.debug(String.format("Received delivery sm (ussd, command_status: %s)", deliverSM.getCommandStatus()));
    } else {
      if (LOGGER.isDebugEnabled())
        LOGGER.debug(String.format("Received delivery sm (command_status: %s)", deliverSM.getCommandStatus()));
      messageType = MessageReceivedEvent.SM_MESSAGE_TYPE;

      final Matcher matcher = DELIVERY_SM_PATTERN.matcher(message);

      if (matcher.matches()) {
        messageId = Integer.parseInt(matcher.group(ID_GROUP), 16);
        final String messageStateString = matcher.group(MESSAGE_STATE_GROUP);

        switch (messageStateString) {
          case MESSAGE_DELIVERED_STATE_STRING:
            messageState = MESSAGE_DELIVERED_STATE;
            break;

          case MESSAGE_EXPIRED_STATE_STRING:
            messageState = MESSAGE_EXPIRED_STATE;
            break;

          case MESSAGE_DELETED_STATE_STRING:
            messageState = MESSAGE_DELETED_STATE;
            break;

          case MESSAGE_UNDELIVERED_STATE_STRING:
            messageState = MESSAGE_UNDELIVERED_STATE;
            break;

          case MESSAGE_ACCEPTED_STATE_STRING:
            messageState = MESSAGE_ACCEPTED_STATE;
            break;

          case MESSAGE_UNKNOWN_STATE_STRING:
            messageState = MESSAGE_UNKNOWN_STATE;
            break;

          case MESSAGE_REJECTED_STATE_STRING:
            messageState = MESSAGE_REJECTED_STATE;
            break;

          default:
            LOGGER.warn(String.format("Unknown message status: %s", messageStateString));
            break;
        }

        final SimpleDateFormat dateFormat = new SimpleDateFormat(DELIVERY_SM_DATE_FORMAT);
        final String dateText = matcher.group(DONE_DATE_GROUP);

        try {
          timestamp = new Timestamp(dateFormat.parse(dateText).getTime());
        } catch (ParseException e) {
          LOGGER.warn(String.format("Unknown date format: %s", dateText), e);
          return;
        }
      }
    }

    if (messageId != null && messageState != null) {
      try {
        deliverSMMessagesCounter.incrementValue();

        if (messageState.equals(MESSAGE_DELIVERED_STATE)) {
          deliveredMessagesCounter.incrementValue();
          EventDispatcher.emit(new DeliveredEvent(messageId, timestamp));
        } else if (messageState.equals(MESSAGE_EXPIRED_STATE)) {
          EventDispatcher.emit(new ExpiredEvent(messageId, timestamp));
        } else if (messageState.equals(MESSAGE_DELETED_STATE)) {
          EventDispatcher.emit(new DeletedEvent(messageId, timestamp));
        } else if (messageState.equals(MESSAGE_UNDELIVERED_STATE)) {
          EventDispatcher.emit(new UndeliveredEvent(messageId, timestamp));
        } else if (messageState.equals(MESSAGE_ACCEPTED_STATE)) {
          EventDispatcher.emit(new AcceptedEvent(messageId, timestamp));
        } else if (messageState.equals(MESSAGE_UNKNOWN_STATE)) {
          EventDispatcher.emit(new UnknownEvent(messageId, timestamp));
        } else if (messageState.equals(MESSAGE_REJECTED_STATE)) {
          EventDispatcher.emit(new RejectedEvent(messageId, timestamp));
        } else {
          LOGGER.warn(String.format("Unknown message status: %s", messageState));
        }
      } catch (InterruptedException ignored) {
      }
    } else {
      final Address source = deliverSM.getSource();
      final Address destination = deliverSM.getDestination();

      final String sourceAddress = source != null ? source.getAddress() : UNKNOWN_STRING;
      final String destinationAddress = destination != null ? destination.getAddress() : UNKNOWN_STRING;

      try {
        receivedMessagesCounter.incrementValue();
        if (LOGGER.isDebugEnabled())
          LOGGER.info(String.format("Message received (source_number:%s, destination_number: %s, message: %s)",
                  sourceAddress, destinationAddress, message));

        EventDispatcher.emit(new MessageReceivedEvent(sourceAddress, destinationAddress, message, messageType));
      } catch (InterruptedException ignored) {
      }
    }
  }

  private AlphabetEncoding GetEncoding(final String name) throws Exception {
    if (name.equalsIgnoreCase("default")) {
      return new DefaultAlphabetEncoding();
    }

    if (name.equalsIgnoreCase("ascii")) {
      return new ASCIIEncoding();
    }

    if (name.equalsIgnoreCase("ucs2")) {
      return new UCS2Encoding();
    }

    if (name.equalsIgnoreCase("hpRoman8")) {
      return new HPRoman8Encoding();
    }

    if (name.equalsIgnoreCase("Latin1")) {
      return new Latin1Encoding();
    }

    if (name.equalsIgnoreCase("utf16-le")) {
      return new UTF16Encoding(false);
    }

    if (name.equalsIgnoreCase("utf16-be")) {
      return new UTF16Encoding(true);
    }

    throw new Exception(String.format("Unknown encoding '%s'", name));
  }

  @Override
  public final String toString() {
    return THREAD_NAME;
  }
}