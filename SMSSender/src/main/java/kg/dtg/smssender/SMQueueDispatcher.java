package kg.dtg.smssender;

import com.adenki.smpp.Address;
import com.adenki.smpp.encoding.ASCIIEncoding;
import com.adenki.smpp.encoding.AlphabetEncoding;
import com.adenki.smpp.encoding.UTF16Encoding;
import com.adenki.smpp.event.ReceiverExceptionEvent;
import com.adenki.smpp.event.SMPPEvent;
import com.adenki.smpp.event.SessionObserver;
import com.adenki.smpp.message.*;
import com.adenki.smpp.message.tlv.Tag;
import com.adenki.smpp.version.SMPPVersion;
import kg.dtg.smssender.Operations.ReplaceOperation;
import kg.dtg.smssender.Operations.Operation;
import kg.dtg.smssender.Operations.SubmitOperation;
import kg.dtg.smssender.events.*;
import kg.dtg.smssender.statistic.IncrementalCounterToken;
import kg.dtg.smssender.statistic.MinMaxCounterToken;
import kg.dtg.smssender.statistic.SoftTime;
import kg.dtg.smssender.utils.StringUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
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

  private static final String MESSAGE_DELIVERED_STATE = "DELIVRD";
  private static final String MESSAGE_EXPIRED_STATE = "EXPIRED";
  private static final String MESSAGE_DELETED_STATE = "DELETED";
  private static final String MESSAGE_UNDELIVERED_STATE = "UNDELIV";
  private static final String MESSAGE_ACCEPTED_STATE = "ACCEPTD";
  private static final String MESSAGE_UNKNOWN_STATE = "UNKNOWN";
  private static final String MESSAGE_REJECTED_STATE = "REJECTD";

  private static final int SMSC_DELIVERY_RECEIPT = 1;
  private static final String LATIN_ALPHABET_PATTERN = "[\u0000-\u007f]+";
  private static final String UNKNOWN_STRING = "<unknown>";
  public final UTF16Encoding UTF_16_ENCODING = new UTF16Encoding(true);
  public final ASCIIEncoding ASCII_ENCODING = new ASCIIEncoding();

  private final BlockingQueue<Operation> pendingOperations = new LinkedBlockingQueue<Operation>();

  private final String host;
  private final int port;
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
  private final ConcurrentMap<Long, ShortMessage> shortMessages = new ConcurrentHashMap<Long, ShortMessage>();

  private final MinMaxCounterToken queueSizeCounter;
  private final IncrementalCounterToken submitedMessagesCounter;
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

  public static boolean canPush() {
    if (SMQueueDispatcher.pendingOperations.size() > SMQueueDispatcher.pauseQueueSize && !SMQueueDispatcher.queryDispatchersPaused) {
      QueryDispatcher.pause();
      SMQueueDispatcher.queryDispatchersPaused = true;
      return false;
    }

    return true;
  }

  private SMQueueDispatcher(final Properties properties) throws Exception {
    this.host = properties.getProperty("smsc.host");
    this.port = Integer.parseInt(properties.getProperty("smsc.port"));

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

    queueSizeCounter = new MinMaxCounterToken("SM Queue dispatcher: Queue size", "count");
    submitedMessagesCounter = new IncrementalCounterToken("SM Queue dispatcher: Submited messages", "count");
    replacedMessagesCounter = new IncrementalCounterToken("SM Queue dispatcher: Replaced messages", "count");
    deliveredMessagesCounter = new IncrementalCounterToken("SM Queue dispatcher: Delivered messages", "count");
    errorMessagesCounter = new IncrementalCounterToken("SM Queue dispatcher: Error messages", "count");
    receivedMessagesCounter = new IncrementalCounterToken("SM Queue dispatcher: Received messages", "count");
    submitSMTimeCounter = new MinMaxCounterToken("SM Queue dispatcher: Submit SM time", "milliseconds");
    replaceSMTimeCounter = new MinMaxCounterToken("SM Queue dispatcher: Replace SM time", "milliseconds");

    new Thread(this).start();
  }

  private void connect() throws Exception {
    smppSession = new com.adenki.smpp.Session(host, port);
    smppSession.addObserver(this);

    final BindTransceiver bindTransceiver = new BindTransceiver();
    bindTransceiver.setVersion(SMPPVersion.VERSION_3_4);
    bindTransceiver.setSystemId(systemId);
    bindTransceiver.setPassword(password);
    bindTransceiver.setSystemType(serviceType);

    smppSession.bind(bindTransceiver);

    state = SMDispatcherState.WAIT_CONNECTION;
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
            smppSession.sendPacket(new EnquireLink());
          } catch (IOException ignored) {
          }

          continue;
        }

        LOGGER.info(String.format("<SMQueue> Operation %s received", currentOperation.getId()));
        final long startTime = SoftTime.getTimestamp();

        if (currentOperation instanceof SubmitOperation) {
          submitMessage((SubmitOperation) currentOperation);
          submitSMTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
        } else if (currentOperation instanceof ReplaceOperation) {
          replaceMessage((ReplaceOperation) currentOperation);
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
          LOGGER.info("SMPP session established");
          state = SMDispatcherState.CONNECTED;
        } else {
          state = SMDispatcherState.NOT_CONNECTED;
          LOGGER.warn(String.format("Cannot bind to sms center, command status: %s", packet.getCommandStatus()));
        }
        break;

      case CommandId.SUBMIT_SM_RESP:
        submitSMResponse((SubmitSMResp) packet);
        break;

      case CommandId.REPLACE_SM_RESP:
        replaceSMResponse((ReplaceSMResp) packet);
        break;

      case CommandId.DELIVER_SM:
        deliverSM((DeliverSM) packet);
        break;

      case CommandId.ENQUIRE_LINK:
        try {
          smppSession.sendPacket(new EnquireLinkResp(packet));
        } catch (IOException e) {
          LOGGER.warn("Cannot send enquire link response", e);
        }
        break;
    }
  }

  @Override
  public final void update(final com.adenki.smpp.Session source, final SMPPEvent event) {
    switch (event.getType()) {
      case SMPPEvent.RECEIVER_START:
        state = SMDispatcherState.CONNECTED;
        break;

      case SMPPEvent.RECEIVER_EXCEPTION:
        ReceiverExceptionEvent receiverExceptionEvent = (ReceiverExceptionEvent) event;
        LOGGER.warn(String.format("Receiver thread exit (state %s)",
                receiverExceptionEvent.getState()), receiverExceptionEvent.getException());
        break;

      case SMPPEvent.RECEIVER_EXIT:
        state = SMDispatcherState.NOT_CONNECTED;
        break;
    }
  }

  private void submitMessage(final SubmitOperation submitOperation) throws InterruptedException {
    LOGGER.info(String.format("Operation %s - submit message", submitOperation.getId()));

    try {
      final Address sourceAddress = new Address(sourceTON, sourceNPI, submitOperation.getSourceNumber());
      final Address destinationAddress = new Address(destinationTON, destinationNPI, submitOperation.getDestinationNumber());

      final String message = submitOperation.getMessage();

      String[] shortMessages;
      final AlphabetEncoding encoding;
      if (message.matches(LATIN_ALPHABET_PATTERN)) {
        encoding = ASCII_ENCODING;
        shortMessages = StringUtils.split(message, 160);
      } else {
        encoding = UTF_16_ENCODING;
        shortMessages = StringUtils.split(message, 70);
      }

      for (final String shortMessageText : shortMessages) {
        final byte[] shortMessageEncoded = encoding.encode(shortMessageText);

        final SubmitSM submitSM = new SubmitSM();
        submitSM.setServiceType(serviceType);
        submitSM.setSource(sourceAddress);
        submitSM.setDestination(destinationAddress);
        submitSM.setRegistered(SMSC_DELIVERY_RECEIPT);
        submitSM.setMessage(shortMessageEncoded);

        if (submitOperation.getMessageType() == SubmitOperation.USSD)
          submitSM.setTLV(Tag.USSD_SERVICE_OP, new byte[]{ussdServiceOpValue});

        final ShortMessage shortMessage = new ShortMessage(shortMessageText);

        smppSession.sendPacket(submitSM);

        LOGGER.info(String.format("Send message %s-%s-%s, %s", submitOperation.getSourceNumber(), submitOperation.getDestinationNumber(), shortMessageText, submitSM));

        final long sequenceNum = submitSM.getSequenceNum();
        pendingResponses.put(sequenceNum, submitOperation);
        this.shortMessages.put(sequenceNum, shortMessage);
      }
    } catch (Exception e) {
      LOGGER.warn("Cannot send message", e);
      state = SMDispatcherState.NOT_CONNECTED;
      errorMessagesCounter.incrementValue();
    }
  }

  private void submitSMResponse(final SubmitSMResp submitSMResp) {
    final long sequenceNum = submitSMResp.getSequenceNum();
    final Operation operation = pendingResponses.remove(sequenceNum);
    final ShortMessage shortMessage = shortMessages.remove(sequenceNum);

    final int messageId = Integer.parseInt(submitSMResp.getMessageId(), 16);

    LOGGER.info(String.format("Received submitSMResponse for message %d with %d status", messageId, submitSMResp.getCommandStatus()));
    try {
      if (submitSMResp.getCommandStatus() == 0) {
        submitedMessagesCounter.incrementValue();
        EventDispatcher.emit(new SubmitedEvent(operation, messageId, shortMessage.getMessage()));
      } else {
        LOGGER.warn(String.format("Cannot submit message (command status: %s)", submitSMResp.getCommandStatus()));
      }

      EventDispatcher.emit(new SubmitSMResponseEvent(messageId, submitSMResp.getCommandStatus()));
    } catch (InterruptedException ignored) {
    } catch (Exception exception) {
      LOGGER.error(String.format("Caught exception '%s'", exception.getMessage()));
    }
  }

  private void replaceMessage(final ReplaceOperation replaceOperation) throws InterruptedException {
    final String operationId = replaceOperation.getId();

    LOGGER.info(String.format("OPeration %s - replace message", operationId));

    try {
      final Address sourceAddress = new Address(sourceTON, sourceNPI, replaceOperation.getSourceNumber());

      final String message = replaceOperation.getMessage();
      String[] shortMessages;
      final AlphabetEncoding encoding;
      if (message.matches(LATIN_ALPHABET_PATTERN)) {
        encoding = ASCII_ENCODING;
        shortMessages = StringUtils.split(message, 160);
      } else {
        encoding = UTF_16_ENCODING;
        shortMessages = StringUtils.split(message, 70);
      }

      final List<ShortMessage> submitedMessages = replaceOperation.getShortMessages();
      int replaceMessageCount = shortMessages.length >= submitedMessages.size() ? submitedMessages.size() : shortMessages.length;

      for (int i = 0; i < replaceMessageCount; i++) {
        final ShortMessage submitedShortMessage = submitedMessages.get(i);

        final String shortMessageText = shortMessages[i];
        final byte[] shortMessageEncoded = encoding.encode(shortMessageText);

        submitedShortMessage.setMessage(shortMessageText);

        final ReplaceSM replaceSM = new ReplaceSM();
        replaceSM.setSource(sourceAddress);
        replaceSM.setMessageId(String.format("%x", submitedShortMessage.getMessageId()));
        replaceSM.setRegistered(SMSC_DELIVERY_RECEIPT);
        replaceSM.setMessage(shortMessageEncoded);

        smppSession.sendPacket(replaceSM);

        this.shortMessages.put(replaceSM.getSequenceNum(), submitedShortMessage);

        LOGGER.info(String.format("Send replace message %s-%s, %s", replaceOperation.getSourceNumber(), replaceOperation.getDestinationNumber(), replaceSM));
      }

      final Address destinationAddress = new Address(destinationTON, destinationNPI, replaceOperation.getDestinationNumber());

      if (shortMessages.length > submitedMessages.size()) {
        for (int i = replaceMessageCount; i < shortMessages.length; i++) {
          final String shortMessageText = shortMessages[i];
          final byte[] shortMessageEncoded = encoding.encode(shortMessageText);

          final SubmitSM submitSM = new SubmitSM();
          submitSM.setSource(sourceAddress);
          submitSM.setDestination(destinationAddress);
          submitSM.setRegistered(SMSC_DELIVERY_RECEIPT);
          submitSM.setMessage(shortMessageEncoded);

          final ShortMessage shortMessage = new ShortMessage(shortMessageText);

          smppSession.sendPacket(submitSM);
          final long sequenceNum = submitSM.getSequenceNum();

          pendingResponses.put(sequenceNum, replaceOperation);
          this.shortMessages.put(sequenceNum, shortMessage);

          LOGGER.info(String.format("Send message %s-%s, %s", replaceOperation.getSourceNumber(), replaceOperation.getDestinationNumber(), submitSM));
        }
      }

    } catch (Exception e) {
      LOGGER.warn("Cannot replace message", e);
      state = SMDispatcherState.NOT_CONNECTED;
      errorMessagesCounter.incrementValue();
    }
  }

  private void replaceSMResponse(final ReplaceSMResp replaceSMResp) {
    final ShortMessage shortMessage = shortMessages.remove(replaceSMResp.getSequenceNum());

    final int messageId = shortMessage.getMessageId();

    LOGGER.info(String.format("Received replaceSMResponse for message %d with %d command status", messageId, replaceSMResp.getCommandStatus()));
    try {
      if (replaceSMResp.getCommandStatus() == 0) {
        replacedMessagesCounter.incrementValue();
        EventDispatcher.emit(new ReplacedEvent(messageId, shortMessage.getMessage()));
      } else {
        LOGGER.warn(String.format("Cannot replace message (message id: %s, command status: %s)", messageId, replaceSMResp.getCommandStatus()));
      }

      EventDispatcher.emit(new ReplaceSMResponseEvent(messageId, replaceSMResp.getCommandStatus()));

    } catch (InterruptedException ignored) {
    } catch (Exception exception) {
      LOGGER.error(String.format("Caught exception '%s'", exception.getMessage()));
    }
  }

  private void deliverSM(final DeliverSM deliverSM) {
    try {
      smppSession.sendPacket(new DeliverSMResp(deliverSM));
    } catch (IOException e) {
      LOGGER.warn("Cannot response DELIVER_SM_RESP", e);
    }

    final String message;
    final byte[] rawMessage = deliverSM.getMessage();
    if (deliverSM.getDataCoding() == OCTET_UNSPECIFIED_CODING) {
      message = UTF_16_ENCODING.decode(rawMessage);
    } else {
      message = ASCII_ENCODING.decode(rawMessage);
    }

    LOGGER.info(String.format("Received delivery sm %s, %d-%d", message, deliverSM.getCommandId(), deliverSM.getCommandStatus()));

    final Matcher matcher = DELIVERY_SM_PATTERN.matcher(message);
    if (matcher.matches()) {
      final int messageId = Integer.parseInt(matcher.group(ID_GROUP));

      final String messageState = matcher.group(MESSAGE_STATE_GROUP);

      try {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DELIVERY_SM_DATE_FORMAT);
        final String dateText = matcher.group(DONE_DATE_GROUP);
        final Timestamp timestamp;

        try {
          timestamp = new Timestamp(dateFormat.parse(dateText).getTime());
        } catch (ParseException e) {
          LOGGER.warn(String.format("Unknown date format: %s", dateText), e);
          return;
        }

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
        LOGGER.info(String.format("Delivery SM should be received event %s-%s-%s", sourceAddress, destinationAddress, message));
        EventDispatcher.emit(new ReceivedEvent(sourceAddress, destinationAddress, message));
      } catch (InterruptedException ignored) {
      }
    }
  }

  @Override
  public final String toString() {
    return THREAD_NAME;
  }
}