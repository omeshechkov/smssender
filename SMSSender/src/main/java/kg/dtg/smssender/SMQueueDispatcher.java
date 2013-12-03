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
  private static final String DELIVERY_SM_DATE_FORMAT = "yyMMddhhmm";

  private static final Pattern DELIVERY_SM_PATTERN = Pattern.compile("^id:(\\d+)(.+)done date:(\\d+)(.+)stat:(\\w+)($|(.+)$)");

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

  private static final int SMSC_DELIVERY_RECEIPT = 1;
  private static final String UNKNOWN_STRING = "<unknown>";

  private final String latinAlphabetPattern;
  public final AlphabetEncoding nonLatinEncoding;
  public final AlphabetEncoding latinEncoding;

  private final BlockingQueue<Operation> pendingOperations = new LinkedBlockingQueue<Operation>();

  private final String sourceHost;
  private final Integer sourcePort;
  private final String destinationHost;
  private final int destinationPort;
  private final String systemId;
  private final String password;
  private final String serviceType;
  private final Byte ussdServiceOpValue;
  private final int sourceTON;
  private final int sourceNPI;
  private final int destinationTON;
  private final int destinationNPI;
  private final int resumeQueueSize;
  private final int pauseQueueSize;
  private final int keepAlive;
  private final int sendInterval;

  private final ConcurrentMap<Long, Operation> pendingResponses = new ConcurrentHashMap<Long, Operation>();

  private final MinMaxCounterToken queueSizeCounter;
  private final IncrementalCounterToken submittedMessagesCounter;
  private final IncrementalCounterToken replacedMessagesCounter;
  private final IncrementalCounterToken deliveredMessagesCounter;
  private final IncrementalCounterToken errorMessagesCounter;
  private final IncrementalCounterToken receivedMessagesCounter;
  private final MinMaxCounterToken submitSMTimeCounter;
  private final MinMaxCounterToken replaceSMTimeCounter;

  private com.adenki.smpp.Session smppSession;
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

    this.sourceTON = Integer.parseInt(properties.getProperty("source.ton"));
    this.sourceNPI = Integer.parseInt(properties.getProperty("source.npi"));

    this.destinationTON = Integer.parseInt(properties.getProperty("destination.ton"));
    this.destinationNPI = Integer.parseInt(properties.getProperty("destination.npi"));

    this.pauseQueueSize = Integer.parseInt(properties.getProperty("smssender.queryDispatcher.pauseQueueSize"));
    this.resumeQueueSize = Integer.parseInt(properties.getProperty("smssender.resumeQueueSize"));

    this.keepAlive = Integer.parseInt(properties.getProperty("smssender.keepAlive"));
    this.sendInterval = 1000 / Integer.parseInt(properties.getProperty("smssender.maxMessagesPerSecond"));

    this.latinAlphabetPattern = properties.getProperty("smssender.latin_message.pattern");
    this.latinEncoding = GetEncoding(properties.getProperty("smssender.latin_message.encoding"));

    this.nonLatinEncoding = GetEncoding(properties.getProperty("smssender.non_latin_message.encoding"));

    queueSizeCounter = new MinMaxCounterToken("SM Queue dispatcher: Queue size", "count");
    submittedMessagesCounter = new IncrementalCounterToken("SM Queue dispatcher: Submited messages", "count");
    replacedMessagesCounter = new IncrementalCounterToken("SM Queue dispatcher: Replaced messages", "count");
    deliveredMessagesCounter = new IncrementalCounterToken("SM Queue dispatcher: Delivered messages", "count");
    errorMessagesCounter = new IncrementalCounterToken("SM Queue dispatcher: Error messages", "count");
    receivedMessagesCounter = new IncrementalCounterToken("SM Queue dispatcher: Received messages", "count");
    submitSMTimeCounter = new MinMaxCounterToken("SM Queue dispatcher: Submit SM time", "milliseconds");
    replaceSMTimeCounter = new MinMaxCounterToken("SM Queue dispatcher: Replace SM time", "milliseconds");

    new Thread(this).start();
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
            if (LOGGER.isDebugEnabled())
              LOGGER.debug("Enquire link");
            smppSession.send(new EnquireLink());
          } catch (IOException ignored) {
          }

          continue;
        }

        if (LOGGER.isDebugEnabled())
          LOGGER.info(String.format("<SMQueue> Operation#%s received", currentOperation.getUid()));

        final long startTime = SoftTime.getTimestamp();

        if (currentOperation instanceof SubmitOperation) {
          submitMessage(currentOperation);
          submitSMTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
        } else if (currentOperation instanceof ReplaceShortMessageOperation) {
          switch (currentOperation.getState()) {
            case OperationState.SUBMITTED:
              cancelMessage(currentOperation);
              break;

            case OperationState.SCHEDULED:
            case OperationState.CANCELLED_TO_REPLACE:
              submitMessage(currentOperation);
              break;
          }

          replaceSMTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
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

      case CommandId.CANCEL_SM_RESP:
        cancelSMResponse((CancelSMResp) packet);
        break;

      case CommandId.DELIVER_SM:
        deliverSM((DeliverSM) packet);
        break;

      case CommandId.ENQUIRE_LINK:
        try {
          smppSession.send(new EnquireLinkResp(packet));
        } catch (IOException e) {
          LOGGER.warn("Cannot send enquire link response", e);
        }
        break;
    }
  }

  @Override
  public final void update(final com.adenki.smpp.Session source, final SMPPEvent event) {
    if (event instanceof ReceiverStartEvent) {
      LOGGER.debug("Receiver started...");
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

      final Address sourceAddress = new Address(sourceTON, sourceNPI, operation.getSourceNumber());
      final Address destinationAddress = new Address(destinationTON, destinationNPI, operation.getDestinationNumber());

      final SubmitSM submitSM = new SubmitSM();
      submitSM.setServiceType(operation.getServiceType());
      submitSM.setSource(sourceAddress);
      submitSM.setDestination(destinationAddress);
      submitSM.setRegistered(SMSC_DELIVERY_RECEIPT);

      final String message;
      if (operation instanceof SubmitOperation) {
        message = ((SubmitOperation) operation).getMessage();
      } else {
        message = ((ReplaceShortMessageOperation) operation).getMessage();
      }

      final AlphabetEncoding encoding;
      if (message.matches(latinAlphabetPattern)) {
        encoding = latinEncoding;
      } else {
        encoding = nonLatinEncoding;
      }


      submitSM.setDataCoding(encoding.getDataCoding());

      if (operation instanceof SubmitUSSDOperation) {
        submitSM.setTLV(Tag.USSD_SERVICE_OP, new byte[]{ussdServiceOpValue});
        submitSM.setMessage(encoding.encode(message));
      } else {
        submitSM.setTLV(Tag.MESSAGE_PAYLOAD, encoding.encode(message));
      }

      smppSession.send(submitSM);

      if (LOGGER.isDebugEnabled())
        LOGGER.debug(String.format("Sending message %s-%s-%s, %s", operation.getSourceNumber(), operation.getDestinationNumber(), message, submitSM));

      final long sequenceNum = submitSM.getSequenceNum();
      pendingResponses.put(sequenceNum, operation);
    } catch (Exception e) {
      LOGGER.warn("Cannot send message", e);
      state = SMDispatcherState.NOT_CONNECTED;
      errorMessagesCounter.incrementValue();
    }
  }

  private void submitSMResponse(final SubmitSMResp submitSMResp) {
    final long sequenceNum = submitSMResp.getSequenceNum();
    final Operation operation = pendingResponses.remove(sequenceNum);

    final int messageId = Integer.parseInt(submitSMResp.getMessageId(), 16);

    LOGGER.info(String.format("Received submitSMResponse for message %d with %d status", messageId, submitSMResp.getCommandStatus()));
    try {
      EventDispatcher.emit(new SubmittedEvent(operation, messageId, submitSMResp.getCommandStatus()));

      if (submitSMResp.getCommandStatus() != 0) {
        LOGGER.warn(String.format("Cannot submit message (command status: %s)", submitSMResp.getCommandStatus()));
        return;
      }

      if (operation instanceof SubmitOperation) {
        submittedMessagesCounter.incrementValue();
      } else {
        replacedMessagesCounter.incrementValue();
      }
    } catch (Exception exception) {
      LOGGER.error(exception);
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

      final Address sourceAddress = new Address(sourceTON, sourceNPI, operation.getSourceNumber());
      final Address destinationAddress = new Address(destinationTON, destinationNPI, operation.getDestinationNumber());

      final CancelSM cancelSM = new CancelSM();
      cancelSM.setServiceType(operation.getServiceType());
      cancelSM.setSource(sourceAddress);
      cancelSM.setDestination(destinationAddress);
      cancelSM.setMessageId(String.format("%X", messageId));

      smppSession.send(cancelSM);

      if (LOGGER.isDebugEnabled())
        LOGGER.debug(String.format("Sending cancel message %s-%s, %s", operation.getSourceNumber(), operation.getDestinationNumber(), cancelSM));

      final long sequenceNum = cancelSM.getSequenceNum();
      pendingResponses.put(sequenceNum, operation);
    } catch (Exception e) {
      LOGGER.warn("Cannot send message", e);
      state = SMDispatcherState.NOT_CONNECTED;

      try {
        errorMessagesCounter.incrementValue();
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
        LOGGER.warn(String.format("Cannot cancel message (command status: %s)", commandStatus));
      }
    } catch (Exception exception) {
      LOGGER.error(exception);
    }
  }

  private void deliverSM(final DeliverSM deliverSM) {
    try {
      smppSession.send(new DeliverSMResp(deliverSM));
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

    Integer messageId = null;
    Integer messageState = null;
    Timestamp timestamp = null;
    if (deliverSM.getTLV(Tag.USSD_SERVICE_OP) != null) {
      messageId = Integer.parseInt(deliverSM.getTLVTable().getString(Tag.RECEIPTED_MESSAGE_ID), 16);
      messageState = deliverSM.getTLVTable().getInt(Tag.MESSAGE_STATE);
      timestamp = new Timestamp(System.currentTimeMillis());

      LOGGER.info(String.format("Received delivery sm (for ussd) %d-%d", deliverSM.getCommandId(), deliverSM.getCommandStatus()));
    } else {
      LOGGER.info(String.format("Received delivery sm %s, %d-%d", message, deliverSM.getCommandId(), deliverSM.getCommandStatus()));
      final Matcher matcher = DELIVERY_SM_PATTERN.matcher(message);
      if (matcher.matches()) {
        messageId = Integer.parseInt(matcher.group(ID_GROUP));
        final String messageStateString = matcher.group(MESSAGE_STATE_GROUP);

        if (messageStateString.equals(MESSAGE_DELIVERED_STATE_STRING)) {
          messageState = MESSAGE_DELIVERED_STATE;
        } else if (messageStateString.equals(MESSAGE_EXPIRED_STATE_STRING)) {
          messageState = MESSAGE_EXPIRED_STATE;
        } else if (messageStateString.equals(MESSAGE_DELETED_STATE_STRING)) {
          messageState = MESSAGE_DELETED_STATE;
        } else if (messageStateString.equals(MESSAGE_UNDELIVERED_STATE_STRING)) {
          messageState = MESSAGE_UNDELIVERED_STATE;
        } else if (messageStateString.equals(MESSAGE_ACCEPTED_STATE_STRING)) {
          messageState = MESSAGE_ACCEPTED_STATE;
        } else if (messageStateString.equals(MESSAGE_UNKNOWN_STATE_STRING)) {
          messageState = MESSAGE_UNKNOWN_STATE;
        } else if (messageStateString.equals(MESSAGE_REJECTED_STATE_STRING)) {
          messageState = MESSAGE_REJECTED_STATE;
        } else {
          LOGGER.warn(String.format("Unknown message status: %s", messageState));
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

    if (messageState != null) {
      try {
        if (messageState.equals(MESSAGE_DELIVERED_STATE)) {
          deliveredMessagesCounter.incrementValue();
          LOGGER.info("Delivery SM is of state DELIVERED");
          EventDispatcher.emit(new DeliveredEvent(messageId, timestamp));
        } else if (messageState.equals(MESSAGE_EXPIRED_STATE)) {
          LOGGER.info("Delivery SM is of state EXPIRED");
          EventDispatcher.emit(new ExpiredEvent(messageId, timestamp));
        } else if (messageState.equals(MESSAGE_DELETED_STATE)) {
          LOGGER.info("Delivery SM is of state DELETED");
          EventDispatcher.emit(new DeletedEvent(messageId, timestamp));
        } else if (messageState.equals(MESSAGE_UNDELIVERED_STATE)) {
          LOGGER.info("Delivery SM is of state UNDELIVERED");
          EventDispatcher.emit(new UndeliveredEvent(messageId, timestamp));
        } else if (messageState.equals(MESSAGE_ACCEPTED_STATE)) {
          LOGGER.info("Delivery SM is of state ACCEPTED");
          EventDispatcher.emit(new AcceptedEvent(messageId, timestamp));
        } else if (messageState.equals(MESSAGE_UNKNOWN_STATE)) {
          LOGGER.info("Delivery SM is of state UNKNOWN");
          EventDispatcher.emit(new UnknownEvent(messageId, timestamp));
        } else if (messageState.equals(MESSAGE_REJECTED_STATE)) {
          LOGGER.info("Delivery SM is of state REJECTED");
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
        LOGGER.info(String.format("Message received %s-%s-%s", sourceAddress, destinationAddress, message));
        EventDispatcher.emit(new MessageReceivedEvent(sourceAddress, destinationAddress, message));
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