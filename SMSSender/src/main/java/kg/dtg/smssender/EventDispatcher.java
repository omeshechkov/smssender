package kg.dtg.smssender;

import kg.dtg.smssender.Operations.OperationState;
import kg.dtg.smssender.db.ConnectionConsumer;
import kg.dtg.smssender.db.ConnectionDispatcherState;
import kg.dtg.smssender.db.ConnectionState;
import kg.dtg.smssender.db.ConnectionToken;
import kg.dtg.smssender.events.*;
import kg.dtg.smssender.statistic.MinMaxCounterToken;
import kg.dtg.smssender.statistic.SoftTime;
import kg.dtg.smssender.utils.Circular;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 4/10/11
 * Time: 6:47 PM
 */
public final class EventDispatcher extends ConnectionConsumer {
  private static final String THREAD_NAME = "Event dispatcher";

  private static final Logger LOGGER = Logger.getLogger(EventDispatcher.class);

  private static Circular<EventDispatcher> eventDispatchers;

  private static final int CALLABLE_STATEMENTS_COUNT = 6;
  private static final int QUERY_STATEMENTS_COUNT = 0;

  private static final int NOTIFY_MESSAGE_RECEIVED_STATEMENT = 0;
  private static final int CHANGE_OPERATION_STATE_STATEMENT = 1;

  private static final int PARAMETER_NOTIFY_MESSAGE_RECEIVED__UID = 1; //in p_uid varchar(36)
  private static final int PARAMETER_NOTIFY_MESSAGE_RECEIVED__TYPE_ID = 2; //in p_type_id int(11)
  private static final int PARAMETER_NOTIFY_MESSAGE_RECEIVED__SOURCE_NUMBER = 3; //in p_source_number varchar(50)
  private static final int PARAMETER_NOTIFY_MESSAGE_RECEIVED__DESTINATION_NUMBER = 4; //in p_destination_number varchar(50)
  private static final int PARAMETER_NOTIFY_MESSAGE_RECEIVED__MESSAGE = 5; //in p_message text
  private static final int PARAMETER_NOTIFY_MESSAGE_RECEIVED__TIMESTAMP = 6; //in p_timestamp timestamp


  private static final int PARAMETER_CHANGE_OPERATION_STATE__UID = 1; //in p_uid varchar(36)
  private static final int PARAMETER_CHANGE_OPERATION_STATE__MESSAGE_ID = 2; //in p_message_id int
  private static final int PARAMETER_CHANGE_OPERATION_STATE__STATE = 3; //in p_state int
  private static final int PARAMETER_CHANGE_OPERATION_STATE__SMPP_STATUS = 4; //in p_smpp_status int
  private static final int PARAMETER_CHANGE_OPERATION_STATE__SMPP_TIMESTAMP = 5; //in p_timestamp timestamp


  private final BlockingQueue<Event> pendingEvents = new LinkedBlockingQueue<Event>();

  private int counter = 0;

  private static final MinMaxCounterToken queueSizeCounter;
  private static final MinMaxCounterToken executionTimeCounter;
  private static final MinMaxCounterToken notifyReceivedTimeCounter;

  static {
    queueSizeCounter = new MinMaxCounterToken("Event dispatcher: Queue size", "count");
    executionTimeCounter = new MinMaxCounterToken("Event dispatcher: Queue size", "milliseconds");
    notifyReceivedTimeCounter = new MinMaxCounterToken("Event dispatcher: Notify received time", "milliseconds");
  }

  public static void initialize(Properties properties) {
    final int eventDispatchersCount = Integer.parseInt(properties.getProperty("smssender.eventDispatcher.count"));

    eventDispatchers = new Circular<EventDispatcher>(eventDispatchersCount);

    for (int i = 0; i < eventDispatchersCount; i++) {
      final EventDispatcher eventDispatcher = new EventDispatcher();
      eventDispatchers.add(eventDispatcher);
    }
  }

  public static void emit(final Event event) throws InterruptedException {
    eventDispatchers.next().pendingEvents.put(event);
  }

  @Override
  public final void connectionTokenAllocated(final ConnectionToken connectionToken) {
    LOGGER.info("Connection for event dispatcher successfully allocated");

    connectionToken.callableStatements = new CallableStatement[CALLABLE_STATEMENTS_COUNT];
    connectionToken.queryStatements = new PreparedStatement[QUERY_STATEMENTS_COUNT];

    final Connection connection = connectionToken.connection;

    try {
      connectionToken.callableStatements[NOTIFY_MESSAGE_RECEIVED_STATEMENT] = connection.prepareCall("call notify_message_received(?, ?, ?, ?, ?, ?)");
      connectionToken.callableStatements[CHANGE_OPERATION_STATE_STATEMENT] = connection.prepareCall("call change_operation_state(?, ?, ?, ?, ?)");

      LOGGER.info("Connection for event dispatcher successfully prepared");
      state = ConnectionDispatcherState.RUNNING;
    } catch (SQLException e) {
      LOGGER.warn("Cannot prepare statements", e);
      connectionToken.connectionState = ConnectionState.NOT_CONNECTED;
    }
  }

  @Override
  protected final void work() throws InterruptedException {
    counter = ++counter % 20;

    if (counter == 19) {
      queueSizeCounter.setValue(pendingEvents.size());
    }

    final Event event = pendingEvents.take();
    if (LOGGER.isDebugEnabled())
      LOGGER.debug(String.format("<ProcessingQueue> %s received", event));

    final long startTime = SoftTime.getTimestamp();

    try {
      if (event instanceof MessageReceivedEvent) {
        final MessageReceivedEvent messageReceivedEvent = (MessageReceivedEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching message received event"));

        final CallableStatement notifyReceivedStatement = connectionToken.callableStatements[NOTIFY_MESSAGE_RECEIVED_STATEMENT];

        notifyReceivedStatement.setString(PARAMETER_NOTIFY_MESSAGE_RECEIVED__UID, UUID.randomUUID().toString());
        notifyReceivedStatement.setInt(PARAMETER_NOTIFY_MESSAGE_RECEIVED__TYPE_ID, messageReceivedEvent.getMessageType());
        notifyReceivedStatement.setString(PARAMETER_NOTIFY_MESSAGE_RECEIVED__SOURCE_NUMBER, messageReceivedEvent.getSourceNumber());
        notifyReceivedStatement.setString(PARAMETER_NOTIFY_MESSAGE_RECEIVED__DESTINATION_NUMBER, messageReceivedEvent.getDestinationNumber());
        notifyReceivedStatement.setString(PARAMETER_NOTIFY_MESSAGE_RECEIVED__MESSAGE, messageReceivedEvent.getMessage());
        notifyReceivedStatement.setTimestamp(PARAMETER_NOTIFY_MESSAGE_RECEIVED__TIMESTAMP, messageReceivedEvent.getTimestamp());

        notifyReceivedStatement.execute();

        notifyReceivedTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
      } else if (event instanceof SubmittingEvent) {
        final SubmittingEvent submittingEvent = (SubmittingEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching submitting event (opereation_id: %s)", submittingEvent.getOperation().getUid()));

        final CallableStatement changeOperationStateStatement = connectionToken.callableStatements[CHANGE_OPERATION_STATE_STATEMENT];

        changeOperationStateStatement.setString(PARAMETER_CHANGE_OPERATION_STATE__UID, submittingEvent.getOperation().getUid());
        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__MESSAGE_ID, Types.INTEGER);
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__STATE, OperationState.SUBMITTING);
        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__SMPP_STATUS, Types.INTEGER);
        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__SMPP_TIMESTAMP, Types.TIMESTAMP);

        changeOperationStateStatement.execute();
      } else if (event instanceof SubmittedEvent) {
        final SubmittedEvent submittedEvent = (SubmittedEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching submitted event (message_id: %s)", submittedEvent.getMessageId()));

        final CallableStatement changeOperationStateStatement = connectionToken.callableStatements[CHANGE_OPERATION_STATE_STATEMENT];

        changeOperationStateStatement.setString(PARAMETER_CHANGE_OPERATION_STATE__UID, submittedEvent.getOperation().getUid());
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__MESSAGE_ID, submittedEvent.getMessageId());
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__STATE, OperationState.SUBMITTED);
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__SMPP_STATUS, submittedEvent.getSmppStatus());
        changeOperationStateStatement.setTimestamp(PARAMETER_CHANGE_OPERATION_STATE__SMPP_TIMESTAMP, submittedEvent.getTimestamp());

        changeOperationStateStatement.execute();
      } else if (event instanceof CancellingEvent) {
        final CancellingEvent cancellingEvent = (CancellingEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching cancelling event (operation_id: %s)", cancellingEvent.getOperation().getUid()));

        final CallableStatement changeOperationStateStatement = connectionToken.callableStatements[CHANGE_OPERATION_STATE_STATEMENT];

        changeOperationStateStatement.setString(PARAMETER_CHANGE_OPERATION_STATE__UID, cancellingEvent.getOperation().getUid());
        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__MESSAGE_ID, Types.INTEGER);
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__STATE, OperationState.CANCELLING);
        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__SMPP_STATUS, Types.INTEGER);
        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__SMPP_TIMESTAMP, Types.TIMESTAMP);

        changeOperationStateStatement.execute();
      } else if (event instanceof CancelledEvent) {
        final CancelledEvent cancelledEvent = (CancelledEvent) event;
        final int messageId = cancelledEvent.getOperation().getMessageId();

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching cancelled event (message_id: %s)", messageId));

        final CallableStatement changeOperationStateStatement = connectionToken.callableStatements[CHANGE_OPERATION_STATE_STATEMENT];

        changeOperationStateStatement.setString(PARAMETER_CHANGE_OPERATION_STATE__UID, cancelledEvent.getOperation().getUid());
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__MESSAGE_ID, messageId);
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__STATE, OperationState.CANCELLED);
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__SMPP_STATUS, cancelledEvent.getSmppStatus());
        changeOperationStateStatement.setTimestamp(PARAMETER_CHANGE_OPERATION_STATE__SMPP_TIMESTAMP, cancelledEvent.getTimestamp());

        changeOperationStateStatement.execute();
      } else if (event instanceof CancellingToReplaceEvent) {
        final CancellingToReplaceEvent cancellingToReplaceEvent = (CancellingToReplaceEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching cancelling to replace event (operation_id: %s)", cancellingToReplaceEvent.getOperation().getUid()));

        final CallableStatement changeOperationStateStatement = connectionToken.callableStatements[CHANGE_OPERATION_STATE_STATEMENT];

        changeOperationStateStatement.setString(PARAMETER_CHANGE_OPERATION_STATE__UID, cancellingToReplaceEvent.getOperation().getUid());
        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__MESSAGE_ID, Types.INTEGER);
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__STATE, OperationState.CANCELLING_TO_REPLACE);
        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__SMPP_STATUS, Types.INTEGER);
        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__SMPP_TIMESTAMP, Types.TIMESTAMP);

        changeOperationStateStatement.execute();
      } else if (event instanceof CancelledToReplaceEvent) {
        final CancelledToReplaceEvent cancelledToReplaceEvent = (CancelledToReplaceEvent) event;
        final int messageId = cancelledToReplaceEvent.getOperation().getMessageId();

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching cancelled to replace event (message_id: %s)", messageId));

        final CallableStatement changeOperationStateStatement = connectionToken.callableStatements[CHANGE_OPERATION_STATE_STATEMENT];

        changeOperationStateStatement.setString(PARAMETER_CHANGE_OPERATION_STATE__UID, cancelledToReplaceEvent.getOperation().getUid());
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__MESSAGE_ID, messageId);
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__STATE, OperationState.CANCELLED_TO_REPLACE);
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__SMPP_STATUS, cancelledToReplaceEvent.getSmppStatus());
        changeOperationStateStatement.setTimestamp(PARAMETER_CHANGE_OPERATION_STATE__SMPP_TIMESTAMP, cancelledToReplaceEvent.getTimestamp());

        changeOperationStateStatement.execute();
      } else if (event instanceof DeliveredEvent) {
        final DeliveredEvent deliveredEvent = (DeliveredEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching delivered event (message_id: %s)", deliveredEvent.getMessageId()));

        final CallableStatement changeOperationStateStatement = connectionToken.callableStatements[CHANGE_OPERATION_STATE_STATEMENT];

        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__UID, Types.VARCHAR);
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__MESSAGE_ID, deliveredEvent.getMessageId());
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__STATE, OperationState.DELIVERED);
        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__SMPP_STATUS, Types.INTEGER);
        changeOperationStateStatement.setTimestamp(PARAMETER_CHANGE_OPERATION_STATE__SMPP_TIMESTAMP, deliveredEvent.getTimestamp());

        changeOperationStateStatement.execute();
      } else if (event instanceof AcceptedEvent) {
        final AcceptedEvent acceptedEvent = (AcceptedEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching accepted event (message_id: %s)", acceptedEvent.getMessageId()));

        final CallableStatement changeOperationStateStatement = connectionToken.callableStatements[CHANGE_OPERATION_STATE_STATEMENT];

        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__UID, Types.VARCHAR);
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__MESSAGE_ID, acceptedEvent.getMessageId());
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__STATE, OperationState.ACCEPTED);
        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__SMPP_STATUS, Types.INTEGER);
        changeOperationStateStatement.setTimestamp(PARAMETER_CHANGE_OPERATION_STATE__SMPP_TIMESTAMP, acceptedEvent.getTimestamp());

        changeOperationStateStatement.execute();
      } else if (event instanceof DeletedEvent) {
        final DeletedEvent deletedEvent = (DeletedEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching deleted event (message_id: %s)", deletedEvent.getMessageId()));

        final CallableStatement changeOperationStateStatement = connectionToken.callableStatements[CHANGE_OPERATION_STATE_STATEMENT];

        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__UID, Types.VARCHAR);
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__MESSAGE_ID, deletedEvent.getMessageId());
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__STATE, OperationState.DELETED);
        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__SMPP_STATUS, Types.INTEGER);
        changeOperationStateStatement.setTimestamp(PARAMETER_CHANGE_OPERATION_STATE__SMPP_TIMESTAMP, deletedEvent.getTimestamp());

        changeOperationStateStatement.execute();
      } else if (event instanceof ExpiredEvent) {
        final ExpiredEvent expiredEvent = (ExpiredEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching expired event (message_id: %s)", expiredEvent.getMessageId()));

        final CallableStatement changeOperationStateStatement = connectionToken.callableStatements[CHANGE_OPERATION_STATE_STATEMENT];

        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__UID, Types.VARCHAR);
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__MESSAGE_ID, expiredEvent.getMessageId());
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__STATE, OperationState.EXPIRED);
        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__SMPP_STATUS, Types.INTEGER);
        changeOperationStateStatement.setTimestamp(PARAMETER_CHANGE_OPERATION_STATE__SMPP_TIMESTAMP, expiredEvent.getTimestamp());

        changeOperationStateStatement.execute();
      } else if (event instanceof RejectedEvent) {
        final RejectedEvent rejectedEvent = (RejectedEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching rejected event (message_id: %s)", rejectedEvent.getMessageId()));

        final CallableStatement changeOperationStateStatement = connectionToken.callableStatements[CHANGE_OPERATION_STATE_STATEMENT];

        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__UID, Types.VARCHAR);
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__MESSAGE_ID, rejectedEvent.getMessageId());
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__STATE, OperationState.REJECTED);
        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__SMPP_STATUS, Types.INTEGER);
        changeOperationStateStatement.setTimestamp(PARAMETER_CHANGE_OPERATION_STATE__SMPP_TIMESTAMP, rejectedEvent.getTimestamp());

        changeOperationStateStatement.execute();
      } else if (event instanceof UndeliveredEvent) {
        final UndeliveredEvent undeliveredEvent = (UndeliveredEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching undelivered event (message_id: %s)", undeliveredEvent.getMessageId()));

        final CallableStatement changeOperationStateStatement = connectionToken.callableStatements[CHANGE_OPERATION_STATE_STATEMENT];

        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__UID, Types.VARCHAR);
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__MESSAGE_ID, undeliveredEvent.getMessageId());
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__STATE, OperationState.UNDELIVERABLE);
        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__SMPP_STATUS, Types.INTEGER);
        changeOperationStateStatement.setTimestamp(PARAMETER_CHANGE_OPERATION_STATE__SMPP_TIMESTAMP, undeliveredEvent.getTimestamp());

        changeOperationStateStatement.execute();
      } else if (event instanceof UnknownEvent) {
        final UnknownEvent unknownEvent = (UnknownEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching unknown event %s-%s", unknownEvent.getMessageId(), unknownEvent.getTimestamp()));

        final CallableStatement changeOperationStateStatement = connectionToken.callableStatements[CHANGE_OPERATION_STATE_STATEMENT];

        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__UID, Types.VARCHAR);
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__MESSAGE_ID, unknownEvent.getMessageId());
        changeOperationStateStatement.setInt(PARAMETER_CHANGE_OPERATION_STATE__STATE, OperationState.UNKNOWN);
        changeOperationStateStatement.setNull(PARAMETER_CHANGE_OPERATION_STATE__SMPP_STATUS, Types.INTEGER);
        changeOperationStateStatement.setTimestamp(PARAMETER_CHANGE_OPERATION_STATE__SMPP_TIMESTAMP, unknownEvent.getTimestamp());

        changeOperationStateStatement.execute();
      }
    } catch (SQLException e) {
      LOGGER.warn("Cannot execute statement", e);
      connectionToken.connectionState = ConnectionState.NOT_CONNECTED;
    }

    executionTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
  }

  @Override
  public final String toString() {
    return THREAD_NAME;
  }
}