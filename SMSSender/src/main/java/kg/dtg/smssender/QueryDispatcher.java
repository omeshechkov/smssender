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

    try (final Connection connection = ConnectionAllocator.getConnection()) {
      try (final CallableStatement lockOperationsStatement = connection.prepareCall("call " + lockOperationsProcedure + "()")) {
        lockOperationsStatement.execute();
      }

      String sql = "select d.uid, d.operation_type_id, d.source_number, d.source_number_ton, " +
              "d.source_number_npi, d.destination_number, d.service_type, d.message, d.service_id, d.state, d.current_seq " +
              "from dispatching d " +
              "where d.worker = connection_id() AND d.query_state = 1";

      final List<Object[]> dispatching = DatabaseFacade.query(connection, sql);

      for (final Object[] dispatchingRow : dispatching) {
        final String operationUid = (String)dispatchingRow[0];
        final int operationType = (int)dispatchingRow[1];

        final Operation operation;

        final String sourceNumber = (String)dispatchingRow[2];
        final Integer sourceNumberTon = (Integer)dispatchingRow[3];
        final Integer sourceNumberNpi = (Integer)dispatchingRow[4];
        final String destinationNumber = (String)dispatchingRow[5];
        final String serviceType = (String)dispatchingRow[6];
        final String message = (String)dispatchingRow[7];
        final Integer serviceId = (Integer)dispatchingRow[8];
        final Integer state = (Integer)dispatchingRow[9];
        final Integer currentSequence = (Integer)dispatchingRow[10];

        if (LOGGER.isInfoEnabled()) {
          LOGGER.info(String.format("Received operation {\n  Operation Id: %s\n  Source number(ton: %s, npi: %s): %s\n  Destination number(ton: %s, npi: %s): %s\n  Message: %s\n  State: %s\n}\n",
                  operationUid, sourceNumberTon, sourceNumberNpi, sourceNumber,
                  destinationTon, destinationNpi, destinationNumber,
                  message, serviceId));
        }

        ShortMessage[] shortMessages = null;

        if (operationType == OperationType.REPLACE_SHORT_MESSAGE || operationType == OperationType.CANCEL_SHORT_MESSAGE) {
          sql = "select sm.id, sm.smpp_state, sm.message_id from short_message sm where sm.dispatching_uid = ? and sm.sequence = ?";
          final List<Object[]> shortMessageRows = DatabaseFacade.query(connection, sql);
          shortMessages = new ShortMessage[shortMessageRows.size()];

          for (int i = 0; i < shortMessageRows.size(); i++) {
            final long id = (Long)shortMessageRows.get(i)[0];
            final int smppState = (Integer)shortMessageRows.get(i)[1];
            final int messageId = (Integer)shortMessageRows.get(i)[2];

            shortMessages[i] = new ShortMessage(id, smppState, messageId);
          }
        }

        switch (operationType) {
          case OperationType.SUBMIT_SHORT_MESSAGE:
            operation = new SubmitMessageOperation(operationUid, sourceNumber, sourceNumberTon, sourceNumberNpi,
                    destinationNumber, destinationTon, destinationNpi, message, serviceType, state);
            break;

          case OperationType.REPLACE_SHORT_MESSAGE:
            operation = new ReplaceMessageOperation(operationUid, sourceNumber, sourceNumberTon, sourceNumberNpi,
                    destinationNumber, destinationTon, destinationNpi, serviceType, state, shortMessages, currentSequence, message);
            break;

          case OperationType.CANCEL_SHORT_MESSAGE:
            operation = new CancelMessageOperation(operationUid, sourceNumber, sourceNumberTon, sourceNumberNpi,
                    destinationNumber, destinationTon, destinationNpi, serviceType, state, shortMessages, currentSequence);
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

      sql = "UPDATE dispatching d SET d.query_state = 2 WHERE d.worker = connection_id()";
      try (final PreparedStatement sealOperationsQuery = connection.prepareStatement(sql)) {
        sealOperationsQuery.executeUpdate();
      }

      connection.commit();
    } catch (final SQLException e) {
      totalQueryTimeCounter.setValue(SoftTime.getTimestamp() - startTime);

      if (e.getErrorCode() != MySQLErrorCodes.ER_LOCK_DEADLOCK && e.getErrorCode() != MySQLErrorCodes.ER_LOCK_WAIT_TIMEOUT) {
        LOGGER.warn("Cannot query operations", e);
      }
      Thread.sleep(workInterval);
      return;
    } catch (final Exception e) {
      totalQueryTimeCounter.setValue(SoftTime.getTimestamp() - startTime);

      LOGGER.warn("Cannot query operations", e);
      Thread.sleep(workInterval);
      return;
    }

    totalQueryTimeCounter.setValue(SoftTime.getTimestamp() - startTime);

    if (operations.size() > 0) {
      for (final Operation operation : operations) {
        SMQueueDispatcher.emit(operation);
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