package kg.dtg.smssender;

import kg.dtg.smssender.Operations.OperationState;
import kg.dtg.smssender.events.*;
import kg.dtg.smssender.statistic.MinMaxCounterToken;
import kg.dtg.smssender.statistic.SoftTime;
import org.apache.log4j.Logger;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 4/10/11
 * Time: 6:47 PM
 */
public final class EventDispatcher extends Dispatcher {
  private static final String THREAD_NAME = "Event dispatcher";

  private static final Logger LOGGER = Logger.getLogger(EventDispatcher.class);

  private static EventDispatcher eventDispatcher;


  private final BlockingQueue<Event> pendingEvents = new LinkedBlockingQueue<>();

  private int counter = 0;

  private static final MinMaxCounterToken queueSizeCounter;
  private static final MinMaxCounterToken executionTimeCounter;
  private static final MinMaxCounterToken notifyReceivedTimeCounter;

  static {
    queueSizeCounter = new MinMaxCounterToken("Event dispatcher: Queue size", "count");
    executionTimeCounter = new MinMaxCounterToken("Event dispatcher: Queue size", "milliseconds");
    notifyReceivedTimeCounter = new MinMaxCounterToken("Event dispatcher: Notify received time", "milliseconds");
  }

  public static void initialize() {
    eventDispatcher = new EventDispatcher();
  }

  public static void emit(final Event event) throws InterruptedException {
    eventDispatcher.pendingEvents.put(event);
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

    try (final Connection connection = ConnectionAllocator.getConnection()) {
      if (event instanceof MessageReceivedEvent) {
        final MessageReceivedEvent messageReceivedEvent = (MessageReceivedEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching message received event"));

        try (final CallableStatement notifyReceivedStatement = connection.prepareCall("call notify_message_received(?, ?, ?, ?, ?, ?)")) {
          notifyReceivedStatement.setString(1, UUID.randomUUID().toString());
          notifyReceivedStatement.setInt(2, messageReceivedEvent.getMessageType());
          notifyReceivedStatement.setString(3, messageReceivedEvent.getSourceNumber());
          notifyReceivedStatement.setString(4, messageReceivedEvent.getDestinationNumber());
          notifyReceivedStatement.setString(5, messageReceivedEvent.getMessage());
          notifyReceivedStatement.setTimestamp(6, messageReceivedEvent.getTimestamp());

          notifyReceivedStatement.execute();
        }

        notifyReceivedTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
      } else if (event instanceof SubmittingEvent) {
        final SubmittingEvent submittingEvent = (SubmittingEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching submitting event (opereation_id: %s)", submittingEvent.getOperation().getUid()));

        try (final CallableStatement changeOperationStateStatement = connection.prepareCall("call change_operation_state(?, ?, ?, ?, ?)")) {
          changeOperationStateStatement.setString(1, submittingEvent.getOperation().getUid());
          changeOperationStateStatement.setNull(2, Types.INTEGER);
          changeOperationStateStatement.setInt(3, OperationState.SUBMITTING);
          changeOperationStateStatement.setNull(4, Types.INTEGER);
          changeOperationStateStatement.setNull(5, Types.TIMESTAMP);

          changeOperationStateStatement.execute();
        }
      } else if (event instanceof SubmittedEvent) {
        final SubmittedEvent submittedEvent = (SubmittedEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching submitted event (message_id: %s)", submittedEvent.getMessageId()));

        try (final CallableStatement changeOperationStateStatement = connection.prepareCall("call change_operation_state(?, ?, ?, ?, ?)")) {
          changeOperationStateStatement.setString(1, submittedEvent.getOperation().getUid());
          changeOperationStateStatement.setInt(2, submittedEvent.getMessageId());
          changeOperationStateStatement.setInt(3, OperationState.SUBMITTED);
          changeOperationStateStatement.setInt(4, submittedEvent.getSmppStatus());
          changeOperationStateStatement.setTimestamp(5, submittedEvent.getTimestamp());

          changeOperationStateStatement.execute();
        }
      } else if (event instanceof CancellingEvent) {
        final CancellingEvent cancellingEvent = (CancellingEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching cancelling event (operation_id: %s)", cancellingEvent.getOperation().getUid()));

        try (final CallableStatement changeOperationStateStatement = connection.prepareCall("call change_operation_state(?, ?, ?, ?, ?)")) {
          changeOperationStateStatement.setString(1, cancellingEvent.getOperation().getUid());
          changeOperationStateStatement.setNull(2, Types.INTEGER);
          changeOperationStateStatement.setInt(3, OperationState.CANCELLING);
          changeOperationStateStatement.setNull(4, Types.INTEGER);
          changeOperationStateStatement.setNull(5, Types.TIMESTAMP);

          changeOperationStateStatement.execute();
        }
      } else if (event instanceof CancelledEvent) {
        final CancelledEvent cancelledEvent = (CancelledEvent) event;
        final int messageId = cancelledEvent.getOperation().getMessageId();

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching cancelled event (message_id: %s)", messageId));

        try (final CallableStatement changeOperationStateStatement = connection.prepareCall("call change_operation_state(?, ?, ?, ?, ?)")) {
          changeOperationStateStatement.setString(1, cancelledEvent.getOperation().getUid());
          changeOperationStateStatement.setInt(2, messageId);
          changeOperationStateStatement.setInt(3, OperationState.CANCELLED);
          changeOperationStateStatement.setInt(4, cancelledEvent.getSmppStatus());
          changeOperationStateStatement.setTimestamp(5, cancelledEvent.getTimestamp());

          changeOperationStateStatement.execute();
        }
      } else if (event instanceof CancellingToReplaceEvent) {
        final CancellingToReplaceEvent cancellingToReplaceEvent = (CancellingToReplaceEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching cancelling to replace event (operation_id: %s)", cancellingToReplaceEvent.getOperation().getUid()));

        try (final CallableStatement changeOperationStateStatement = connection.prepareCall("call change_operation_state(?, ?, ?, ?, ?)")) {
          changeOperationStateStatement.setString(1, cancellingToReplaceEvent.getOperation().getUid());
          changeOperationStateStatement.setNull(2, Types.INTEGER);
          changeOperationStateStatement.setInt(3, OperationState.CANCELLING_TO_REPLACE);
          changeOperationStateStatement.setNull(4, Types.INTEGER);
          changeOperationStateStatement.setNull(5, Types.TIMESTAMP);

          changeOperationStateStatement.execute();
        }
      } else if (event instanceof CancelledToReplaceEvent) {
        final CancelledToReplaceEvent cancelledToReplaceEvent = (CancelledToReplaceEvent) event;
        final int messageId = cancelledToReplaceEvent.getOperation().getMessageId();

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching cancelled to replace event (message_id: %s)", messageId));

        try (final CallableStatement changeOperationStateStatement = connection.prepareCall("call change_operation_state(?, ?, ?, ?, ?)")) {
          changeOperationStateStatement.setString(1, cancelledToReplaceEvent.getOperation().getUid());
          changeOperationStateStatement.setInt(2, messageId);
          changeOperationStateStatement.setInt(3, OperationState.CANCELLED_TO_REPLACE);
          changeOperationStateStatement.setInt(4, cancelledToReplaceEvent.getSmppStatus());
          changeOperationStateStatement.setTimestamp(5, cancelledToReplaceEvent.getTimestamp());

          changeOperationStateStatement.execute();
        }
      } else if (event instanceof DeliveredEvent) {
        final DeliveredEvent deliveredEvent = (DeliveredEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching delivered event (message_id: %s)", deliveredEvent.getMessageId()));

        try (final CallableStatement changeOperationStateStatement = connection.prepareCall("call change_operation_state(?, ?, ?, ?, ?)")) {
          changeOperationStateStatement.setNull(1, Types.VARCHAR);
          changeOperationStateStatement.setInt(2, deliveredEvent.getMessageId());
          changeOperationStateStatement.setInt(3, OperationState.DELIVERED);
          changeOperationStateStatement.setNull(4, Types.INTEGER);
          changeOperationStateStatement.setTimestamp(5, deliveredEvent.getTimestamp());

          changeOperationStateStatement.execute();

        }
      } else if (event instanceof AcceptedEvent) {
        final AcceptedEvent acceptedEvent = (AcceptedEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching accepted event (message_id: %s)", acceptedEvent.getMessageId()));

        try (final CallableStatement changeOperationStateStatement = connection.prepareCall("call change_operation_state(?, ?, ?, ?, ?)")) {
          changeOperationStateStatement.setNull(1, Types.VARCHAR);
          changeOperationStateStatement.setInt(2, acceptedEvent.getMessageId());
          changeOperationStateStatement.setInt(3, OperationState.ACCEPTED);
          changeOperationStateStatement.setNull(4, Types.INTEGER);
          changeOperationStateStatement.setTimestamp(5, acceptedEvent.getTimestamp());

          changeOperationStateStatement.execute();
        }
      } else if (event instanceof DeletedEvent) {
        final DeletedEvent deletedEvent = (DeletedEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching deleted event (message_id: %s)", deletedEvent.getMessageId()));

        try (final CallableStatement changeOperationStateStatement = connection.prepareCall("call change_operation_state(?, ?, ?, ?, ?)")) {
          changeOperationStateStatement.setNull(1, Types.VARCHAR);
          changeOperationStateStatement.setInt(2, deletedEvent.getMessageId());
          changeOperationStateStatement.setInt(3, OperationState.DELETED);
          changeOperationStateStatement.setNull(4, Types.INTEGER);
          changeOperationStateStatement.setTimestamp(5, deletedEvent.getTimestamp());

          changeOperationStateStatement.execute();
        }
      } else if (event instanceof ExpiredEvent) {
        final ExpiredEvent expiredEvent = (ExpiredEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching expired event (message_id: %s)", expiredEvent.getMessageId()));

        try (final CallableStatement changeOperationStateStatement = connection.prepareCall("call change_operation_state(?, ?, ?, ?, ?)")) {
          changeOperationStateStatement.setNull(1, Types.VARCHAR);
          changeOperationStateStatement.setInt(2, expiredEvent.getMessageId());
          changeOperationStateStatement.setInt(3, OperationState.EXPIRED);
          changeOperationStateStatement.setNull(4, Types.INTEGER);
          changeOperationStateStatement.setTimestamp(5, expiredEvent.getTimestamp());

          changeOperationStateStatement.execute();
        }
      } else if (event instanceof RejectedEvent) {
        final RejectedEvent rejectedEvent = (RejectedEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching rejected event (message_id: %s)", rejectedEvent.getMessageId()));

        try (final CallableStatement changeOperationStateStatement = connection.prepareCall("call change_operation_state(?, ?, ?, ?, ?)")) {
          changeOperationStateStatement.setNull(1, Types.VARCHAR);
          changeOperationStateStatement.setInt(2, rejectedEvent.getMessageId());
          changeOperationStateStatement.setInt(3, OperationState.REJECTED);
          changeOperationStateStatement.setNull(4, Types.INTEGER);
          changeOperationStateStatement.setTimestamp(5, rejectedEvent.getTimestamp());

          changeOperationStateStatement.execute();
        }
      } else if (event instanceof UndeliveredEvent) {
        final UndeliveredEvent undeliveredEvent = (UndeliveredEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching undelivered event (message_id: %s)", undeliveredEvent.getMessageId()));

        try (final CallableStatement changeOperationStateStatement = connection.prepareCall("call change_operation_state(?, ?, ?, ?, ?)")) {
          changeOperationStateStatement.setNull(1, Types.VARCHAR);
          changeOperationStateStatement.setInt(2, undeliveredEvent.getMessageId());
          changeOperationStateStatement.setInt(3, OperationState.UNDELIVERABLE);
          changeOperationStateStatement.setNull(4, Types.INTEGER);
          changeOperationStateStatement.setTimestamp(5, undeliveredEvent.getTimestamp());

          changeOperationStateStatement.execute();
        }
      } else if (event instanceof UnknownEvent) {
        final UnknownEvent unknownEvent = (UnknownEvent) event;

        if (LOGGER.isDebugEnabled())
          LOGGER.debug(String.format("Dispatching unknown event %s-%s", unknownEvent.getMessageId(), unknownEvent.getTimestamp()));

        try (final CallableStatement changeOperationStateStatement = connection.prepareCall("call change_operation_state(?, ?, ?, ?, ?)")) {
          changeOperationStateStatement.setNull(1, Types.VARCHAR);
          changeOperationStateStatement.setInt(2, unknownEvent.getMessageId());
          changeOperationStateStatement.setInt(3, OperationState.UNKNOWN);
          changeOperationStateStatement.setNull(4, Types.INTEGER);
          changeOperationStateStatement.setTimestamp(5, unknownEvent.getTimestamp());

          changeOperationStateStatement.execute();
        }
      }

      connection.commit();
    } catch (SQLException e) {
      LOGGER.warn(String.format("Cannot dispatch %s", event), e);
    }

    executionTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
  }

  @Override
  public final String toString() {
    return THREAD_NAME;
  }
}