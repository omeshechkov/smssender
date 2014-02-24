package kg.dtg.smssender;

import kg.dtg.smssender.Operations.*;
import kg.dtg.smssender.statistic.MinMaxCounterToken;
import kg.dtg.smssender.statistic.SoftTime;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 4/9/11
 * Time: 8:00 PM
 */
public final class QueryDispatcher extends Dispatcher {
  private static Logger LOGGER = Logger.getLogger(QueryDispatcher.class);

  private static final String THREAD_NAME = "Query dispatcher";

  private static QueryDispatcher queryDispatcher;
  private static String lockOperationsProcedure;
  private static int pollInterval;
  private static int workInterval;

  private final int destinationTon;
  private final int destinationNpi;

  private NodeState nodeState = NodeState.WAITING_STATE;

  private static final MinMaxCounterToken totalQueryTimeCounter;

  static {
    totalQueryTimeCounter = new MinMaxCounterToken("Query dispatcher: Total query time", "milliseconds");
  }

  public static void initialize(Properties properties) {
    lockOperationsProcedure = properties.getProperty("smssender.lock_operations_proc");
    pollInterval = Integer.parseInt(properties.getProperty("smssender.queryDispatcher.poll_interval"));
    workInterval = Integer.parseInt(properties.getProperty("smssender.queryDispatcher.work_interval"));

    queryDispatcher = new QueryDispatcher(properties);
  }

  public static void pause() {
    queryDispatcher.setState(DispatcherState.PAUSE);
  }

  public static void resume() {
    queryDispatcher.setState(DispatcherState.RUNNING);
  }

  public static void setState(boolean active) {
    if (queryDispatcher.nodeState != NodeState.ACTIVE && active) {
      LOGGER.info(String.format("Current node changed state to active"));
    } else if (queryDispatcher.nodeState != NodeState.PASSIVE && !active) {
      LOGGER.info(String.format("Current node changed state to passive"));
    }

    queryDispatcher.nodeState = active ? NodeState.ACTIVE : NodeState.PASSIVE;
  }

  private QueryDispatcher(Properties properties) {
    this.destinationTon = Integer.parseInt(properties.getProperty("destination.ton"));
    this.destinationNpi = Integer.parseInt(properties.getProperty("destination.npi"));
  }

  @Override
  protected final void work() throws InterruptedException {
    if (!SMQueueDispatcher.canEmit() || nodeState != NodeState.ACTIVE) {
      Thread.sleep(pollInterval);
      return;
    }

    final long startTime = SoftTime.getTimestamp();
    final List<Operation> operations = new LinkedList<>();

    try (Connection connection = ConnectionAllocator.getConnection()) {
      try(final CallableStatement lockOperationsStatement = connection.prepareCall( "call " + lockOperationsProcedure + "()")) {
        lockOperationsStatement.execute();
      }

      String sql = "SELECT d.uid, d.operation_type_id, d.source_number, d.source_number_ton, " +
              "d.source_number_npi, d.destination_number, d.service_type, d.message, d.message_id, d.service_id, d.state " +
              "FROM dispatching d " +
              "WHERE d.worker = connection_id() AND d.query_state = 1";
      try (final PreparedStatement selectOperationsQuery = connection.prepareStatement(sql)) {
        try (final ResultSet resultSet = selectOperationsQuery.executeQuery()) {
          while (resultSet.next()) {
            final String operationUid = resultSet.getString(1);
            final int operationType = resultSet.getInt(2);

            final Operation operation;

            final String sourceNumber = resultSet.getString(3);
            final Integer sourceNumberTon = resultSet.getInt(4);
            final Integer sourceNumberNpi = resultSet.getInt(5);
            final String destinationNumber = resultSet.getString(6);
            final String serviceType = resultSet.getString(7);
            final String message = resultSet.getString(8);
            final Integer messageId = resultSet.getInt(9);
            final Integer serviceId = resultSet.getInt(10);
            final Integer state = resultSet.getInt(11);

            LOGGER.info(String.format("Received operation {\n  Operation Id: %s\n  Source number(ton: %s, npi: %s): %s\n  Destination number(ton: %s, npi: %s): %s\n  Message: %s\n  State: %s\n}\n",
                    operationUid, sourceNumberTon, sourceNumberNpi, sourceNumber,
                    destinationTon, destinationNpi, destinationNumber,
                    message, serviceId));

            switch (operationType) {
              case OperationType.SUBMIT_SHORT_MESSAGE:
                operation = new SubmitShortMessageOperation(operationUid, sourceNumber, sourceNumberTon, sourceNumberNpi,
                        destinationNumber, destinationTon, destinationNpi, message, serviceType, state);
                break;

              case OperationType.REPLACE_SHORT_MESSAGE:
                operation = new ReplaceShortMessageOperation(operationUid, sourceNumber, sourceNumberTon, sourceNumberNpi,
                        destinationNumber, destinationTon, destinationNpi, serviceType, state, messageId, message);
                break;

              case OperationType.CANCEL_SHORT_MESSAGE:
                operation = new CancelShortMessageOperation(operationUid, sourceNumber, sourceNumberTon, sourceNumberNpi,
                        destinationNumber, destinationTon, destinationNpi, serviceType, state, messageId);
                break;

              case OperationType.SUBMIT_USSD:
                operation = new SubmitUSSDOperation(operationUid, sourceNumber, sourceNumberTon, sourceNumberNpi,
                        destinationNumber, destinationTon, destinationNpi, message, serviceType, state);
                break;

              default:
                LOGGER.warn(String.format("Invalid operation#%s", operationType));
                continue;
            }

            operations.add(operation);
          }
        }
      }

      sql = "UPDATE dispatching d SET d.query_state = 2 WHERE d.worker = connection_id()";
      try (final PreparedStatement sealOperationsQuery = connection.prepareStatement(sql)){
        sealOperationsQuery.executeUpdate();
      }

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