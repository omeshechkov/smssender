package kg.dtg.smssender;

import kg.dtg.smssender.Operations.*;
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

  private static final int QUERY_STATEMENTS_COUNT = 2;
  private static final int CALLABLE_STATEMENTS_COUNT = 1;

  private static final int LOCK_OPERATIONS_FOR_QUERY_STATEMENT = 0;
  private static final int SELECT_OPERATIONS_QUERY = 1;
  private static final int SEAL_OPERATIONS_QUERY = 2;

  private static final int OPERATION_UID_COLUMN = 1;
  private static final int OPERATION_TYPE_ID_COLUMN = 2;
  private static final int OPERATION_SOURCE_NUMBER_COLUMN = 3;
  private static final int OPERATION_DESTINATION_NUMBER_COLUMN = 4;
  private static final int OPERATION_SERVICE_TYPE_COLUMN = 5;
  private static final int OPERATION_MESSAGE_COLUMN = 6;
  private static final int OPERATION_MESSAGE_ID_COLUMN = 7;
  private static final int OPERATION_SERVICE_ID_COLUMN = 8;
  private static final int OPERATION_STATE_COLUMN = 9;


  private static final MinMaxCounterToken totalQueryTimeCounter;

  static {
    totalQueryTimeCounter = new MinMaxCounterToken("Query dispatcher: Total query time", "milliseconds");
  }

  public static void initialize(Properties properties) {
    final int queryDispatchersCount = Integer.parseInt(properties.getProperty("smssender.queryDispatcher.count"));

    LOGGER.debug(String.format("Query dispatchers count: %s", queryDispatchersCount));

    queryDispatchers = new Circular<QueryDispatcher>(queryDispatchersCount);

    for (int i = 0; i < queryDispatchersCount; i++) {
      final QueryDispatcher queryDispatcher = new QueryDispatcher();
      queryDispatchers.add(queryDispatcher);
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

      connectionToken.queryStatements = new PreparedStatement[QUERY_STATEMENTS_COUNT];
      connectionToken.callableStatements = new CallableStatement[CALLABLE_STATEMENTS_COUNT];

      connectionToken.callableStatements[LOCK_OPERATIONS_FOR_QUERY_STATEMENT] = connection.prepareCall(
              "call lock_operations_for_query()"
      );

      connectionToken.queryStatements[SELECT_OPERATIONS_QUERY] = connection.prepareStatement(
              "SELECT d.uid, d.operation_type_id, d.source_number, d.destination_number, d.service_type, d.message, d.message_id, d.service_id, ds.state " +
                      "FROM dispatching d " +
                      "LEFT JOIN dispatching_state ds ON ds.uid = d.uid AND ds.is_actual = true" +
                      "WHERE d.worker = connection_id() AND d.query_state = 1"
      );

      connectionToken.queryStatements[SEAL_OPERATIONS_QUERY] = connection.prepareStatement(
              "UPDATE dispatching d SET d.query_state = 2 WHERE d.worker = connection_id()"
      );

      LOGGER.info("Connection for query dispatcher is successfully prepared");
    } catch (SQLException e) {
      LOGGER.warn("Cannot prepare statements", e);
      connectionToken.connectionState = ConnectionState.NOT_CONNECTED;
    }
  }

  @Override
  protected final void work() throws InterruptedException {
    if (!SMQueueDispatcher.canEmit()) {
      Thread.sleep(100);
      return;
    }

    final PreparedStatement lockOperationsStatement = connectionToken.callableStatements[LOCK_OPERATIONS_FOR_QUERY_STATEMENT];
    final PreparedStatement selectOperationsQuery = connectionToken.queryStatements[SELECT_OPERATIONS_QUERY];
    final PreparedStatement sealOperationsQuery = connectionToken.callableStatements[SEAL_OPERATIONS_QUERY];

    ResultSet resultSet = null;

    final long startTime = SoftTime.getTimestamp();
    final List<Operation> operations = new LinkedList<Operation>();

    try {
      lockOperationsStatement.execute();

      resultSet = selectOperationsQuery.executeQuery();
      while (resultSet.next()) {
        final String operationUid = resultSet.getString(OPERATION_UID_COLUMN);
        final Integer operationType = resultSet.getInt(OPERATION_TYPE_ID_COLUMN);

        final Operation operation;

        final String sourceNumber = resultSet.getString(OPERATION_SOURCE_NUMBER_COLUMN);
        final String destinationNumber = resultSet.getString(OPERATION_DESTINATION_NUMBER_COLUMN);
        final String message = resultSet.getString(OPERATION_MESSAGE_COLUMN);
        final Integer messageId = resultSet.getInt(OPERATION_MESSAGE_ID_COLUMN);
        final String serviceType = resultSet.getString(OPERATION_SERVICE_TYPE_COLUMN);
        final Integer serviceId = resultSet.getInt(OPERATION_SERVICE_ID_COLUMN);
        final Integer state = resultSet.getInt(OPERATION_STATE_COLUMN);

        LOGGER.info(String.format("Received operation {\n  Operation Id: %s\n  Source number: %s\n  Destination number: %s\n  Message: %s\n  State: %s\n}\n",
                operationUid, sourceNumber, destinationNumber, message, serviceId));

        switch (operationType) {
          case OperationType.SUBMIT_SHORT_MESSAGE:
            operation = new SubmitShortMessageOperation(operationUid, sourceNumber, destinationNumber, message, serviceType, state);
            break;

          case OperationType.REPLACE_SHORT_MESSAGE:
            operation = new ReplaceShortMessageOperation(operationUid, sourceNumber, destinationNumber, serviceType, state, messageId, message);
            break;

          case OperationType.CANCEL_SHORT_MESSAGE:
            operation = new CancelShortMessageOperation(operationUid, sourceNumber, destinationNumber, serviceType, state, messageId);
            break;

          case OperationType.SUBMIT_USSD:
            operation = new SubmitUSSDOperation(operationUid, sourceNumber, destinationNumber, message, serviceType, state);
            break;

          default:
            LOGGER.warn(String.format("Invalid operation#%s", operationType));
            continue;
        }

        operations.add(operation);
      }

      sealOperationsQuery.executeUpdate();
    } catch (SQLException e) {
      connectionToken.connectionState = ConnectionState.NOT_CONNECTED;
      LOGGER.warn("Cannot query operations, reallocate connection is scheduled", e);
    } finally {
      if (resultSet != null) {
        try {
          resultSet.close();
        } catch (SQLException ignored) {
        }
      }
    }

    for (final Operation operation : operations) {
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