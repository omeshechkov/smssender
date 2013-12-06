package kg.dtg.smssender;

import kg.dtg.smssender.Operations.*;
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
public final class QueryDispatcher extends Dispatcher {
  private static Logger LOGGER = Logger.getLogger(QueryDispatcher.class);

  private static final String THREAD_NAME = "Query dispatcher";

  private static Circular<QueryDispatcher> queryDispatchers;
  private static String lockOperationsProcedure;
  private static int pollInterval;
  private static int workInterval;


  private static final MinMaxCounterToken totalQueryTimeCounter;

  static {
    totalQueryTimeCounter = new MinMaxCounterToken("Query dispatcher: Total query time", "milliseconds");
  }

  public static void initialize(Properties properties) {
    final int queryDispatchersCount = Integer.parseInt(properties.getProperty("smssender.queryDispatcher.count"));
    lockOperationsProcedure = properties.getProperty("smssender.lock_operations_proc");
    pollInterval = Integer.parseInt(properties.getProperty("smssender.queryDispatcher.poll_interval"));
    workInterval = Integer.parseInt(properties.getProperty("smssender.queryDispatcher.work_interval"));

    queryDispatchers = new Circular<>(queryDispatchersCount);

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
        queryConnectionDispatcher.setState(DispatcherState.PAUSE);
      }
    }
  }

  public static void resume() {
    //noinspection SynchronizeOnNonFinalField
    synchronized (queryDispatchers) {
      final QueryDispatcher[] queryDispatchers = QueryDispatcher.queryDispatchers.toArray(QueryDispatcher[].class);
      for (final QueryDispatcher queryConnectionDispatcher : queryDispatchers) {
        queryConnectionDispatcher.setState(DispatcherState.RUNNING);
      }
    }
  }

  private QueryDispatcher() {
  }

  @Override
  protected final void work() throws InterruptedException {
    if (!SMQueueDispatcher.canEmit()) {
      Thread.sleep(pollInterval);
      return;
    }

    final long startTime = SoftTime.getTimestamp();
    final List<Operation> operations = new LinkedList<Operation>();

    try (Connection connection = ConnectionAllocator.getConnection()) {
      final CallableStatement lockOperationsStatement = connection.prepareCall(
              "call " + lockOperationsProcedure + "()"
      );

      lockOperationsStatement.execute();
      lockOperationsStatement.close();

      final PreparedStatement selectOperationsQuery = connection.prepareStatement(
              "SELECT d.uid, d.operation_type_id, d.source_number, d.destination_number, d.service_type, d.message, d.message_id, d.service_id, d.state " +
                      "FROM dispatching d " +
                      "WHERE d.worker = connection_id() AND d.query_state = 1"
      );

      try (final ResultSet resultSet = selectOperationsQuery.executeQuery()) {
        while (resultSet.next()) {
          final String operationUid = resultSet.getString(1);
          final int operationType = resultSet.getInt(2);

          final Operation operation;

          final String sourceNumber = resultSet.getString(3);
          final String destinationNumber = resultSet.getString(4);
          final String message = resultSet.getString(6);
          final Integer messageId = resultSet.getInt(7);
          final String serviceType = resultSet.getString(5);
          final Integer serviceId = resultSet.getInt(8);
          final Integer state = resultSet.getInt(9);

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
      }

      selectOperationsQuery.close();

      final PreparedStatement sealOperationsQuery = connection.prepareStatement("UPDATE dispatching d SET d.query_state = 2 WHERE d.worker = connection_id()");
      sealOperationsQuery.executeUpdate();
      sealOperationsQuery.close();

      connection.commit();
    } catch (Exception e) {
      LOGGER.warn("Cannot query operations", e);
    }

    if (operations.size() > 0) {
      for (final Operation operation : operations) {
        SMQueueDispatcher.emit(operation);
        totalQueryTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
      }

      Thread.sleep(workInterval);
    } else {
      Thread.sleep(pollInterval);
    }
  }

  @Override
  public final String toString() {
    return THREAD_NAME;
  }
}