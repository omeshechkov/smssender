package kg.dtg.smssender;

import kg.dtg.smssender.db.ConnectionConsumer;
import kg.dtg.smssender.db.ConnectionDispatcherState;
import kg.dtg.smssender.db.ConnectionState;
import kg.dtg.smssender.db.ConnectionToken;
import kg.dtg.smssender.statistic.MinMaxCounterToken;
import kg.dtg.smssender.statistic.SoftTime;
import kg.dtg.smssender.utils.Circular;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 4/9/11
 * Time: 8:00 PM
 */
public final class QueryDispatcher extends ConnectionConsumer {
  private static Logger LOGGER = Logger.getLogger(QueryDispatcher.class);

  private static final String THREAD_NAME = "Query dispatcher";

  private static Circular<QueryDispatcher> queryDispatchers;

  private static final int PREPARED_STATEMENTS_COUNT = 3;
  private static final int CALLABLE_STATEMENTS_COUNT = 1;

  private static final int QUERY_MESSAGES_STATEMENT = 0;

  private static final int QUERY_BATCH_STATEMENT = 0;
  private static final int QUERY_SESSIONS_SHORT_MESSAGES_STATEMENT = 1;
  private static final int QUERY_TRUNCATE_BATCH_STATEMENT = 2;

  private static final int MESSAGE_SOURCE_NUMBER_COLUMN = 1;
  private static final int MESSAGE_DESTINATION_NUMBER_COLUMN = 2;
  private static final int MESSAGE_MESSAGE_COLUMN = 3;
  private static final int MESSAGE_STATE_COLUMN = 4;
  private static final int MESSAGE_UID_COLUMN = 5;

  private static final int SHORT_MESSAGE__PARAMETER_SESSION_UID = 1;
  private static final int SHORT_MESSAGE__MESSAGE_ID_COLUMN = 1;


  private static final MinMaxCounterToken executeQueryTimeCounter;
  private static final MinMaxCounterToken totalQueryTimeCounter;

  static {
    executeQueryTimeCounter = new MinMaxCounterToken("Query dispatcher: Execute SQL query time", "milliseconds");
    totalQueryTimeCounter = new MinMaxCounterToken("Query dispatcher: Total query time", "milliseconds");
  }

  public static void initialize(Properties properties) {
    final int queryDispatchersCount = Integer.parseInt(properties.getProperty("smssender.queryDispatcher.count"));

    queryDispatchers = new Circular<QueryDispatcher>(queryDispatchersCount);

    for (int i = 0; i < queryDispatchersCount; i++) {
      final QueryDispatcher connectionDispatcher = new QueryDispatcher();
      queryDispatchers.add(connectionDispatcher);
    }
  }

  public static void pause() {
    //noinspection SynchronizeOnNonFinalField
    synchronized (queryDispatchers) {
      final QueryDispatcher[] queryDispatchers = QueryDispatcher.queryDispatchers.toArray(QueryDispatcher[].class);
      for (final QueryDispatcher queryConnectionDispatcher : queryDispatchers) {
        queryConnectionDispatcher.setState(ConnectionDispatcherState.PAUSE);
      }
    }
  }

  public static void resume() {
    //noinspection SynchronizeOnNonFinalField
    synchronized (queryDispatchers) {
      final QueryDispatcher[] queryDispatchers = QueryDispatcher.queryDispatchers.toArray(QueryDispatcher[].class);
      for (final QueryDispatcher queryConnectionDispatcher : queryDispatchers) {
        queryConnectionDispatcher.setState(ConnectionDispatcherState.RUNNING);
      }
    }
  }

  private QueryDispatcher() {
  }

  @Override
  public final void connectionTokenAllocated(final ConnectionToken connectionToken) {
    try {
      LOGGER.info("Connection for query dispatcher is successfully allocated");

      final Connection connection = connectionToken.connection;

      connectionToken.preparedStatements = new PreparedStatement[PREPARED_STATEMENTS_COUNT];
      connectionToken.callableStatements = new CallableStatement[CALLABLE_STATEMENTS_COUNT];

      connectionToken.callableStatements[QUERY_MESSAGES_STATEMENT] = connection.prepareCall("call query_messages()");

      connectionToken.preparedStatements[QUERY_BATCH_STATEMENT] = connection.prepareStatement("select `source_number`, `destination_number`, `message`, `state`, `uid` from `batch`");
      connectionToken.preparedStatements[QUERY_SESSIONS_SHORT_MESSAGES_STATEMENT] = connection.prepareStatement("select t.message_id from `message` t where t.session_uid = ?");
      connectionToken.preparedStatements[QUERY_TRUNCATE_BATCH_STATEMENT] = connection.prepareStatement("truncate table `batch`");

      final PreparedStatement truncateBatchStatement = connectionToken.preparedStatements[QUERY_TRUNCATE_BATCH_STATEMENT];
      try {
        truncateBatchStatement.execute();
      } catch (SQLException e) {
        LOGGER.warn("Could not truncate batch table", e);
      }

      LOGGER.info("Connection for query dispatcher is successfully prepared");
    } catch (SQLException e) {
      LOGGER.warn("Cannot prepare statements", e);
      connectionToken.connectionState = ConnectionState.NOT_CONNECTED;
    }
  }

  @Override
  protected final void work() throws InterruptedException {
    if (!SMQueueDispatcher.canPush()) {
      Thread.sleep(100);
      return;
    }

    final PreparedStatement queryMessagesStatement = connectionToken.callableStatements[QUERY_MESSAGES_STATEMENT];
    final PreparedStatement queryBatchStatement = connectionToken.preparedStatements[QUERY_BATCH_STATEMENT];
    ResultSet resultSet = null;

    final long startTime = SoftTime.getTimestamp();
    final List<Operation> operations = new LinkedList<Operation>();

    try {
      queryMessagesStatement.execute();
      executeQueryTimeCounter.setValue(SoftTime.getTimestamp() - startTime);

      resultSet = queryBatchStatement.executeQuery();
      while (resultSet.next()) {
        final Session session = new Session(resultSet.getString(MESSAGE_UID_COLUMN));
        final Operation operation;

        final String sourceNumber = resultSet.getString(MESSAGE_SOURCE_NUMBER_COLUMN);
        final String destinationNumber = resultSet.getString(MESSAGE_DESTINATION_NUMBER_COLUMN);
        final String message = resultSet.getString(MESSAGE_MESSAGE_COLUMN);
        final Integer state = resultSet.getInt(MESSAGE_STATE_COLUMN);

        LOGGER.info(String.format("Received data {\n  Session: %s\n  Source number: %s\n  Destination number: %s\n  Message: %s\n  State: %s\n}\n",
                session.getUid(), sourceNumber, destinationNumber, message, state));

        switch (state) {
          case MessageState.REPLACE_STATE:
          case MessageState.SCHEDULED_STATE:
            operation = new Operation(session, sourceNumber, destinationNumber, message, state);
            break;

          default:
            LOGGER.warn(String.format("Invalid operation state (session id: %s, state: %s)", session.getUid(), state));
            continue;
        }

        operations.add(operation);
      }
    } catch (SQLException e) {
      connectionToken.connectionState = ConnectionState.NOT_CONNECTED;
      LOGGER.warn("Cannot query messages, reallocate connection is scheduled", e);
    } finally {
      if (resultSet != null) {
        try {
          resultSet.close();
        } catch (SQLException ignored) {
        }
      }
    }

    for (final Operation operation : operations) {
      final Session session = operation.getSession();

      if (operation.getState() == MessageState.REPLACE_STATE) {
        final PreparedStatement sessionsShortMessagesState = connectionToken.preparedStatements[QUERY_SESSIONS_SHORT_MESSAGES_STATEMENT];

        try {
          sessionsShortMessagesState.setString(SHORT_MESSAGE__PARAMETER_SESSION_UID, session.getUid());
          resultSet = sessionsShortMessagesState.executeQuery();
          while (resultSet.next()) {
            final int messageId = resultSet.getInt(SHORT_MESSAGE__MESSAGE_ID_COLUMN);
            session.getShortMessages().add(new ShortMessage(messageId));
          }
        } catch (SQLException e) {
          connectionToken.connectionState = ConnectionState.NOT_CONNECTED;

          LOGGER.warn(String.format("Cannot query short messages for session %s", session.getUid()), e);
        } finally {
          if (resultSet != null) {
            try {
              resultSet.close();
            } catch (SQLException ignored) {
            }
          }
        }
      }

      LOGGER.info(String.format("Session %s started", session.getUid()));
      SMQueueDispatcher.emit(operation);
    }

    totalQueryTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
    Thread.sleep(100);
  }

  @Override
  public final String toString() {
    return THREAD_NAME;
  }
}