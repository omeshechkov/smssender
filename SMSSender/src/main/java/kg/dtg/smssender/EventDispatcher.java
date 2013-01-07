package kg.dtg.smssender;

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
  private static final int PREPARED_STATEMENTS_COUNT = 1;

  private static final int NOTIFY_RECEIVED_STATEMENT = 0;
  private static final int SUBMIT_SHORT_MESSAGE_STATEMENT = 1;
  private static final int REPLACE_SHORT_MESSAGE_STATEMENT = 2;
  private static final int CHANGE_SHORT_MESSAGE_STATE_STATEMENT = 3;
  private static final int CHANGE_MESSAGE_STATE_STATEMENT = 4;
  private static final int CHANGE_SM_COMMAND_STATUS_EX_STATEMENT = 5;

  private static final int SESSION_SHORT_MESSAGES_STATEMENT = 0;

  private static final int PARAMETER_NOTIFY_RECEIVED__SOURCE_NUMBER = 1; //in p_source_number varchar(50)
  private static final int PARAMETER_NOTIFY_RECEIVED__DESTINATION_NUMBER = 2; //in p_destination_number varchar(50)
  private static final int PARAMETER_NOTIFY_RECEIVED__MESSAGE = 3; //in p_message varchar(999)
  private static final int PARAMETER_NOTIFY_RECEIVED__TIMESTAMP = 4; //in p_timestamp timestamp

  private static final int PARAMETER_SUBMIT_SHORT_MESSAGE__SESSION_UID = 1; //in p_session_id int
  private static final int PARAMETER_SUBMIT_SHORT_MESSAGE__MESSAGE_ID = 2; //in p_message_id int
  private static final int PARAMETER_SUBMIT_SHORT_MESSAGE__MESSAGE = 3; //in p_message varchar(999)
  private static final int PARAMETER_SUBMIT_SHORT_MESSAGE__TIMESTAMP = 4; //in p_timestamp timestamp

  private static final int PARAMETER_REPLACE_SHORT_MESSAGE__MESSAGE_ID = 1; //in p_message_id int
  private static final int PARAMETER_REPLACE_SHORT_MESSAGE__MESSAGE = 2; //in p_message varchar(999)

  private static final int PARAMETER_CHANGE_SHORT_MESSAGE_STATE__MESSAGE_ID = 1; //in p_message_id int
  private static final int PARAMETER_CHANGE_SHORT_MESSAGE_STATE__TIMESTAMP = 2; //in p_timestamp timestamp
  private static final int PARAMETER_CHANGE_SHORT_MESSAGE_STATE__STATE = 3; //in p_state int
  private static final int PARAMETER_CHANGE_SHORT_MESSAGE_STATE__SESSION_UID = 4; //out p_session_id int

  private static final int PARAMETER_CHANGE_MESSAGE_STATE__SESSION_ID = 1; //in p_session_id int
  private static final int PARAMETER_CHANGE_MESSAGE_STATE__SUBMIT_TIMESTAMP = 2; //in p_submit_timestamp timestamp
  private static final int PARAMETER_CHANGE_MESSAGE_STATE__DELIVER_TIMESTAMP = 3; //in p_deliver_timestamp timestamp
  private static final int PARAMETER_CHANGE_MESSAGE_STATE__STATE = 4; //in p_state int

  private static final int PARAMETER_CHANGE_SM_COMMAND_STATUS_EX_MESSAGE_ID = 1;
  private static final int PARAMETER_CHANGE_SM_COMMAND_STATUS_EX_COMMAND_STATUS=2;
  private static final int PARAMETER_CHANGE_SM_COMMAND_STATUS_EX_COMMAND_TIMESTAMP=3;


  private static final int SHORT_MESSAGE__PARAMETER_SESSION_UID = 1;

  private static final int SHORT_MESSAGE__STATE_COLUMN = 1;
  private static final int SHORT_MESSAGE__SUBMIT_TIMESTAMP_COLUMN = 2;
  private static final int SHORT_MESSAGE__DELIVERY_TIMESTAMP_COLUMN = 3;

  private static final int SHORT_MESSAGE_SUBMITTED_STATE = 0;
  private static final int SHORT_MESSAGE_DELIVERED_STATE = 1;
  private static final int SHORT_MESSAGE_EXPIRED_STATE = 2;
  private static final int SHORT_MESSAGE_DELETED_STATE = 3;
  private static final int SHORT_MESSAGE_UNDELIVERABLE_STATE = 4;
  private static final int SHORT_MESSAGE_ACCEPTED_STATE = 5;
  private static final int SHORT_MESSAGE_UNKNOWN_STATE = 6;
  private static final int SHORT_MESSAGE_REJECTED_STATE = 7;

  private static final int MESSAGE_DELIVERED_STATE = 2;
  private static final int MESSAGE_UNDELIVERED_STATE = 4;

  private final BlockingQueue<Event> pendingEvents = new LinkedBlockingQueue<Event>();

  private int counter = 0;

  private static final MinMaxCounterToken queueSizeCounter;
  private static final MinMaxCounterToken executionTimeCounter;
  private static final MinMaxCounterToken updateStatusTimeCounter;
  private static final MinMaxCounterToken notifyReceivedTimeCounter;

  static {
    queueSizeCounter = new MinMaxCounterToken("Event dispatcher: Queue size", "count");
    executionTimeCounter = new MinMaxCounterToken("Event dispatcher: Queue size", "milliseconds");
    updateStatusTimeCounter = new MinMaxCounterToken("Event dispatcher: Update status time", "milliseconds");
    notifyReceivedTimeCounter = new MinMaxCounterToken("Event dispatcher: Notify received time", "milliseconds");
  }

  public static void initialize(Properties properties) {
    final int eventDispatchersCount = Integer.parseInt(properties.getProperty("smssender.eventDispatcher.count"));

    eventDispatchers = new Circular<EventDispatcher>(eventDispatchersCount);

    for (int i = 0; i < eventDispatchersCount; i++) {
      final EventDispatcher connectionDispatcher = new EventDispatcher();
      eventDispatchers.add(connectionDispatcher);
    }
  }

  public static void emit(final Event event) throws InterruptedException {
    eventDispatchers.next().pendingEvents.put(event);
  }

  @Override
  public final void connectionTokenAllocated(final ConnectionToken connectionToken) {
    LOGGER.info("Connection for event dispatcher successfully allocated");

    connectionToken.callableStatements = new CallableStatement[CALLABLE_STATEMENTS_COUNT];
    connectionToken.preparedStatements = new PreparedStatement[PREPARED_STATEMENTS_COUNT];

    final Connection connection = connectionToken.connection;

    try {
      connectionToken.callableStatements[NOTIFY_RECEIVED_STATEMENT] = connection.prepareCall("call notify_received_ex(?, ?, ?, ?)");
      connectionToken.callableStatements[SUBMIT_SHORT_MESSAGE_STATEMENT] = connection.prepareCall("call submit_short_message_ex(?, ?, ?, ?)");
      connectionToken.callableStatements[REPLACE_SHORT_MESSAGE_STATEMENT] = connection.prepareCall("call replace_short_message_ex(?, ?)");
      connectionToken.callableStatements[CHANGE_SHORT_MESSAGE_STATE_STATEMENT] = connection.prepareCall("call change_short_message_state_ex(?, ?, ?, ?)");
      connectionToken.callableStatements[CHANGE_MESSAGE_STATE_STATEMENT] = connection.prepareCall("call change_message_state_ex(?, ?, ?, ?)");
      connectionToken.callableStatements[CHANGE_SM_COMMAND_STATUS_EX_STATEMENT] = connection.prepareCall("call change_sm_command_status_ex(?,?,?)");

      connectionToken.preparedStatements[SESSION_SHORT_MESSAGES_STATEMENT] = connection.prepareStatement("select t.state, t.submit_timestamp, t.delivery_timestamp from `message` t where t.session_uid = ?");

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
      if (event instanceof ReceivedEvent) {
        final ReceivedEvent receivedTicket = (ReceivedEvent) event;

        final CallableStatement notifyReceivedStatement = connectionToken.callableStatements[NOTIFY_RECEIVED_STATEMENT];
        notifyReceivedStatement.setString(PARAMETER_NOTIFY_RECEIVED__SOURCE_NUMBER, receivedTicket.getSourceNumber());
        notifyReceivedStatement.setString(PARAMETER_NOTIFY_RECEIVED__DESTINATION_NUMBER, receivedTicket.getDestinationNumber());
        notifyReceivedStatement.setString(PARAMETER_NOTIFY_RECEIVED__MESSAGE, receivedTicket.getMessage());
        notifyReceivedStatement.setTimestamp(PARAMETER_NOTIFY_RECEIVED__TIMESTAMP, receivedTicket.getTimestamp());
        notifyReceivedStatement.execute();
        notifyReceivedTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
        LOGGER.info(String.format("Dispatching received event %s-%s-%s", receivedTicket.getSourceNumber(), receivedTicket.getDestinationNumber(), receivedTicket.getMessage()));
      } else if (event instanceof SubmitedEvent) {
        final SubmitedEvent submitedEvent = (SubmitedEvent) event;

        final CallableStatement submitSMStatement = connectionToken.callableStatements[SUBMIT_SHORT_MESSAGE_STATEMENT];
        submitSMStatement.setString(PARAMETER_SUBMIT_SHORT_MESSAGE__SESSION_UID, submitedEvent.getSession().getUid());
        submitSMStatement.setInt(PARAMETER_SUBMIT_SHORT_MESSAGE__MESSAGE_ID, submitedEvent.getMessageId());
        submitSMStatement.setString(PARAMETER_SUBMIT_SHORT_MESSAGE__MESSAGE, submitedEvent.getMessage());
        submitSMStatement.setTimestamp(PARAMETER_SUBMIT_SHORT_MESSAGE__TIMESTAMP, submitedEvent.getTimestamp());
        submitSMStatement.execute();
        LOGGER.info(String.format("Dispatching submited event %s-%s-%s", submitedEvent.getMessageId(), submitedEvent.getSession().getUid(), submitedEvent.getMessage()));
        updateStatusTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
      } else if (event instanceof DeliveredEvent) {
        final DeliveredEvent deliveredEvent = (DeliveredEvent) event;
        changeMessageStatus(deliveredEvent.getMessageId(), deliveredEvent.getTimestamp(), SHORT_MESSAGE_DELIVERED_STATE);
        LOGGER.info(String.format("Dispatching delivered event %s-%s", deliveredEvent.getMessageId(), deliveredEvent.getTimestamp()));
        updateStatusTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
      } else if (event instanceof AcceptedEvent) {
        final AcceptedEvent acceptedEvent = (AcceptedEvent) event;
        changeMessageStatus(acceptedEvent.getMessageId(), acceptedEvent.getTimestamp(), SHORT_MESSAGE_ACCEPTED_STATE);
        LOGGER.info(String.format("Dispatching accepted event %s-%s", acceptedEvent.getMessageId(), acceptedEvent.getTimestamp()));
        updateStatusTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
      } else if (event instanceof DeletedEvent) {
        final DeletedEvent deletedEvent = (DeletedEvent) event;
        LOGGER.info(String.format("Dispatching deleted event %s-%s", deletedEvent.getMessageId(), deletedEvent.getTimestamp()));
        changeMessageStatus(deletedEvent.getMessageId(), deletedEvent.getTimestamp(), SHORT_MESSAGE_DELETED_STATE);
      } else if (event instanceof ExpiredEvent) {
        final ExpiredEvent expiredEvent = (ExpiredEvent) event;
        LOGGER.info(String.format("Dispatching expired event %s-%s", expiredEvent.getMessageId(), expiredEvent.getTimestamp()));
        changeMessageStatus(expiredEvent.getMessageId(), expiredEvent.getTimestamp(), SHORT_MESSAGE_EXPIRED_STATE);
      } else if (event instanceof RejectedEvent) {
        final RejectedEvent rejectedEvent = (RejectedEvent) event;
        LOGGER.info(String.format("Dispatching rejected event %s-%s", rejectedEvent.getMessageId(), rejectedEvent.getTimestamp()));
        changeMessageStatus(rejectedEvent.getMessageId(), rejectedEvent.getTimestamp(), SHORT_MESSAGE_REJECTED_STATE);
      } else if (event instanceof UndeliveredEvent) {
        final UndeliveredEvent undeliveredEvent = (UndeliveredEvent) event;
        LOGGER.info(String.format("Dispatching undelivered event %s-%s", undeliveredEvent.getMessageId(), undeliveredEvent.getTimestamp()));
        changeMessageStatus(undeliveredEvent.getMessageId(), undeliveredEvent.getTimestamp(), SHORT_MESSAGE_UNDELIVERABLE_STATE);
      } else if (event instanceof UnknownEvent) {
        final UnknownEvent unknownEvent = (UnknownEvent) event;
        LOGGER.info(String.format("Dispatching unknown event %s-%s", unknownEvent.getMessageId(), unknownEvent.getTimestamp()));
        changeMessageStatus(unknownEvent.getMessageId(), unknownEvent.getTimestamp(), SHORT_MESSAGE_UNKNOWN_STATE);
      } else if (event instanceof ReplacedEvent) {
        final ReplacedEvent replacedEvent = (ReplacedEvent) event;

        LOGGER.info(String.format("Dispatching replaced event %s-%s", replacedEvent.getMessageId(), replacedEvent.getMessageId()));

        final CallableStatement replaceSMStatement = connectionToken.callableStatements[REPLACE_SHORT_MESSAGE_STATEMENT];
        replaceSMStatement.setInt(PARAMETER_REPLACE_SHORT_MESSAGE__MESSAGE_ID, replacedEvent.getMessageId());
        replaceSMStatement.setString(PARAMETER_REPLACE_SHORT_MESSAGE__MESSAGE, replacedEvent.getMessage());
        replaceSMStatement.execute();
      } else if (event instanceof SubmitSMResponseEvent) {
          final SubmitSMResponseEvent submitSMResponseEvent = (SubmitSMResponseEvent) event;

          LOGGER.info(String.format("Dispatching submitSMResponse event %s-%s-%s", submitSMResponseEvent.getMessageId(), submitSMResponseEvent.getCommandStatus(), submitSMResponseEvent.getCommandTimestamp()));

          final CallableStatement changeSMCommandStatusStatement = connectionToken.callableStatements[CHANGE_SM_COMMAND_STATUS_EX_STATEMENT];
          changeSMCommandStatusStatement.setInt(PARAMETER_CHANGE_SM_COMMAND_STATUS_EX_MESSAGE_ID, submitSMResponseEvent.getMessageId());
          changeSMCommandStatusStatement.setInt(PARAMETER_CHANGE_SM_COMMAND_STATUS_EX_COMMAND_STATUS, submitSMResponseEvent.getCommandStatus());
          changeSMCommandStatusStatement.setTimestamp(PARAMETER_CHANGE_SM_COMMAND_STATUS_EX_COMMAND_TIMESTAMP, submitSMResponseEvent.getCommandTimestamp());
          changeSMCommandStatusStatement.execute();
      } else if(event instanceof ReplaceSMResponseEvent) {
          final ReplaceSMResponseEvent replaceSMResponseEvent = (ReplaceSMResponseEvent) event;

          LOGGER.info(String.format("Dispatching replaceSMResponseEvent event %s-%s-%s", replaceSMResponseEvent.getMessageId(), replaceSMResponseEvent.getCommandStatus(), replaceSMResponseEvent.getCommandTimestamp()));

          final CallableStatement changeSMCommandStatusStatement = connectionToken.callableStatements[CHANGE_SM_COMMAND_STATUS_EX_STATEMENT];
          changeSMCommandStatusStatement.setInt(PARAMETER_CHANGE_SM_COMMAND_STATUS_EX_MESSAGE_ID, replaceSMResponseEvent.getMessageId());
          changeSMCommandStatusStatement.setInt(PARAMETER_CHANGE_SM_COMMAND_STATUS_EX_COMMAND_STATUS, replaceSMResponseEvent.getCommandStatus());
          changeSMCommandStatusStatement.setTimestamp(PARAMETER_CHANGE_SM_COMMAND_STATUS_EX_COMMAND_TIMESTAMP, replaceSMResponseEvent.getCommandTimestamp());
          changeSMCommandStatusStatement.execute();
      }
    } catch (SQLException e) {
      LOGGER.warn("Cannot execute statement", e);
      connectionToken.connectionState = ConnectionState.NOT_CONNECTED;
    }

    executionTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
  }

  private void changeMessageStatus(final int messageId, final Timestamp timestamp, final int shortMessageState) throws SQLException {
    final CallableStatement changeSMStateStatement = connectionToken.callableStatements[CHANGE_SHORT_MESSAGE_STATE_STATEMENT];
    changeSMStateStatement.setInt(PARAMETER_CHANGE_SHORT_MESSAGE_STATE__MESSAGE_ID, messageId);
    changeSMStateStatement.setTimestamp(PARAMETER_CHANGE_SHORT_MESSAGE_STATE__TIMESTAMP, timestamp);
    changeSMStateStatement.setInt(PARAMETER_CHANGE_SHORT_MESSAGE_STATE__STATE, shortMessageState);
    changeSMStateStatement.registerOutParameter(PARAMETER_CHANGE_SHORT_MESSAGE_STATE__SESSION_UID, Types.CHAR, 36);
    changeSMStateStatement.execute();

    final String sessionId = changeSMStateStatement.getString(PARAMETER_CHANGE_SHORT_MESSAGE_STATE__SESSION_UID);

    if (sessionId == null || sessionId.equals("")) {
      LOGGER.warn(String.format("Cannot find session for message id: %s", messageId));
      return;
    }

    final PreparedStatement messagesStateStatement = connectionToken.preparedStatements[SESSION_SHORT_MESSAGES_STATEMENT];
    messagesStateStatement.setString(SHORT_MESSAGE__PARAMETER_SESSION_UID, sessionId);
    ResultSet resultSet = null;

    Timestamp submitTimestamp = null;
    Timestamp deliverTimestamp = null;

    int totalCount = 0;
    int messagesWereResponsedCount = 0;

    boolean isMessageDelivered = true;

    try {
      resultSet = messagesStateStatement.executeQuery();

      while (resultSet.next()) {
        final int state = resultSet.getInt(SHORT_MESSAGE__STATE_COLUMN);

        totalCount++;
        if (state != SHORT_MESSAGE_SUBMITTED_STATE) {
          isMessageDelivered &= state == SHORT_MESSAGE_DELIVERED_STATE || state == SHORT_MESSAGE_ACCEPTED_STATE;

          messagesWereResponsedCount++;
        }

        submitTimestamp = resultSet.getTimestamp(SHORT_MESSAGE__SUBMIT_TIMESTAMP_COLUMN);
        deliverTimestamp = resultSet.getTimestamp(SHORT_MESSAGE__DELIVERY_TIMESTAMP_COLUMN);
      }
    } finally {
      if (resultSet != null) {
        try {
          resultSet.close();
        } catch (SQLException ignored) {
        }
      }
    }

    if (messagesWereResponsedCount == totalCount) {
      final int messageState = isMessageDelivered ? MESSAGE_DELIVERED_STATE : MESSAGE_UNDELIVERED_STATE;

      final CallableStatement changeMessageStateStatement = connectionToken.callableStatements[CHANGE_MESSAGE_STATE_STATEMENT];
      changeMessageStateStatement.setString(PARAMETER_CHANGE_MESSAGE_STATE__SESSION_ID, sessionId);
      changeMessageStateStatement.setTimestamp(PARAMETER_CHANGE_MESSAGE_STATE__SUBMIT_TIMESTAMP, submitTimestamp);
      changeMessageStateStatement.setTimestamp(PARAMETER_CHANGE_MESSAGE_STATE__DELIVER_TIMESTAMP, deliverTimestamp);

      changeMessageStateStatement.setInt(PARAMETER_CHANGE_MESSAGE_STATE__STATE, messageState);
      changeMessageStateStatement.execute();
    }
  }

  @Override
  public final String toString() {
    return THREAD_NAME;
  }
}