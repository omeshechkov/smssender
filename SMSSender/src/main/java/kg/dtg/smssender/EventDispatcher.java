package kg.dtg.smssender;

import kg.dtg.smssender.Operations.OperationState;
import kg.dtg.smssender.Operations.ShortMessageState;
import kg.dtg.smssender.events.*;
import kg.dtg.smssender.statistic.IncrementalCounterToken;
import kg.dtg.smssender.statistic.MinMaxCounterToken;
import kg.dtg.smssender.statistic.SoftTime;
import org.apache.log4j.Logger;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 4/10/11
 * Time: 6:47 PM
 */
public final class EventDispatcher extends Dispatcher {
  private static final String THREAD_NAME = "Event dispatcher";

  private static final Logger LOGGER = Logger.getLogger(EventDispatcher.class);

  private static final Object emitSyncObject = new Object();
  private static EventDispatcher[] eventDispatchers;
  private static int currentEventDispatcher = 0;

  private final BlockingQueue<Event> pendingEvents = new LinkedBlockingQueue<>();

  private int counter = 0;

  private final int deadlockRetryAttempts;
  private final long deadlockRetryInterval;

  private final MinMaxCounterToken queueSizeCounter;
  private final MinMaxCounterToken executionTimeCounter;
  private final MinMaxCounterToken eventProcessingTimeCounter;

  private final IncrementalCounterToken deadlockCounter;

  private final MinMaxCounterToken messageReceivedEventProcessingTimeCounter;
  private final MinMaxCounterToken submittedEventProcessingTimeCounter;
  private final MinMaxCounterToken cancelledEventProcessingTimeCounter;
  private final MinMaxCounterToken cancelledToReplaceEventProcessingTimeCounter;

  private final MinMaxCounterToken submittingShortMessageEventProcessingTimeCounter;
  private final MinMaxCounterToken submittedShortMessageEventProcessingTimeCounter;
  private final MinMaxCounterToken cancellingShortMessageEventProcessingTimeCounter;
  private final MinMaxCounterToken cancelledShortMessageEventProcessingTimeCounter;
  private final MinMaxCounterToken deliveredShortMessageEventProcessingTimeCounter;
  private final MinMaxCounterToken acceptedEventProcessingTimeCounter;
  private final MinMaxCounterToken deletedEventProcessingTimeCounter;
  private final MinMaxCounterToken expiredEventProcessingTimeCounter;
  private final MinMaxCounterToken rejectedEventProcessingTimeCounter;
  private final MinMaxCounterToken undeliveredEventProcessingTimeCounter;
  private final MinMaxCounterToken unknownEventProcessingTimeCounter;

  private Connection connection = null;

  public static void initialize(final Properties properties) throws Exception {
    eventDispatchers = new EventDispatcher[1];

    for (int i = 0; i < eventDispatchers.length; i++) {
      eventDispatchers[i] = new EventDispatcher(properties, i + 1);
    }
  }

  public static void emit(final Event event) throws InterruptedException {
    synchronized (emitSyncObject) {
      eventDispatchers[currentEventDispatcher].pendingEvents.put(event);

      currentEventDispatcher++;
      if (currentEventDispatcher > eventDispatchers.length - 1) {
        currentEventDispatcher = 0;
      }
    }
  }

  private EventDispatcher(final Properties properties, int id) throws Exception {
    this.deadlockRetryAttempts = Integer.parseInt(properties.getProperty("smssender.deadlock_retry_attempts"));
    this.deadlockRetryInterval = Long.parseLong(properties.getProperty("smssender.deadlock_retry_interval"));

    queueSizeCounter = new MinMaxCounterToken(String.format("Event dispatcher %s: Queue size", id), "count");
    executionTimeCounter = new MinMaxCounterToken(String.format("Event dispatcher %s: Execution time", id), "milliseconds");
    eventProcessingTimeCounter = new MinMaxCounterToken(String.format("Event dispatcher %s: Event processing time", id), "milliseconds");

    deadlockCounter = new IncrementalCounterToken(String.format("Event dispatcher %s: Deadlocks count", id), "count");

    messageReceivedEventProcessingTimeCounter = new MinMaxCounterToken(String.format("Event dispatcher %s: MessageReceivedEvent Processing time", id), "milliseconds");
    submittedEventProcessingTimeCounter = new MinMaxCounterToken(String.format("Event dispatcher %s: SubmittedEvent Processing time", id), "milliseconds");
    cancelledEventProcessingTimeCounter = new MinMaxCounterToken(String.format("Event dispatcher %s: CancelledEvent Processing time", id), "milliseconds");
    cancelledToReplaceEventProcessingTimeCounter = new MinMaxCounterToken(String.format("Event dispatcher %s: CancelledToReplaceEvent Processing time", id), "milliseconds");

    submittingShortMessageEventProcessingTimeCounter = new MinMaxCounterToken(String.format("Event dispatcher %s: SubmittingShortMessageEvent Processing time", id), "milliseconds");
    submittedShortMessageEventProcessingTimeCounter = new MinMaxCounterToken(String.format("Event dispatcher %s: SubmittedShortMessageEvent Processing time", id), "milliseconds");
    cancellingShortMessageEventProcessingTimeCounter = new MinMaxCounterToken(String.format("Event dispatcher %s: CancellingShortMessageEvent Processing time", id), "milliseconds");
    cancelledShortMessageEventProcessingTimeCounter = new MinMaxCounterToken(String.format("Event dispatcher %s: CancelledShortMessageEvent Processing time", id), "milliseconds");
    deliveredShortMessageEventProcessingTimeCounter = new MinMaxCounterToken(String.format("Event dispatcher %s: DeliveredEvent Processing time", id), "milliseconds");
    acceptedEventProcessingTimeCounter = new MinMaxCounterToken(String.format("Event dispatcher %s: AcceptedEvent Processing time", id), "milliseconds");
    deletedEventProcessingTimeCounter = new MinMaxCounterToken(String.format("Event dispatcher %s: DeletedEvent Processing time", id), "milliseconds");
    expiredEventProcessingTimeCounter = new MinMaxCounterToken(String.format("Event dispatcher %s: ExpiredEvent Processing time", id), "milliseconds");
    rejectedEventProcessingTimeCounter = new MinMaxCounterToken(String.format("Event dispatcher %s: RejectedEvent Processing time", id), "milliseconds");
    undeliveredEventProcessingTimeCounter = new MinMaxCounterToken(String.format("Event dispatcher %s: UndeliveredEvent Processing time", id), "milliseconds");
    unknownEventProcessingTimeCounter = new MinMaxCounterToken(String.format("Event dispatcher %s: UnknownEvent Processing time", id), "milliseconds");
  }

  @Override
  protected final void work() throws InterruptedException {
    counter = ++counter % 20;

    if (counter == 19) {
      queueSizeCounter.setValue(pendingEvents.size());
    }

    final Event event = pendingEvents.poll(100, TimeUnit.MILLISECONDS);

    if (event == null) {
      if (connection != null) {
        try {
          DatabaseFacade.query(connection, "select 1");
        } catch (SQLException e) {
          connection = null;
        }
      }
      return;
    }

    if (LOGGER.isDebugEnabled())
      LOGGER.debug(String.format("<ProcessingQueue> %s received", event));

    final long startTime = SoftTime.getTimestamp();
    boolean completed;
    int deadlockCounter = 0;

    do {
      try {
        if (connection == null) {
          connection = ConnectionAllocator.getConnection();
        }

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

            messageReceivedEventProcessingTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
          }
        } else if (event instanceof SubmittedEvent) {
          final SubmittedEvent submittingEvent = (SubmittedEvent) event;

          if (LOGGER.isDebugEnabled())
            LOGGER.debug(String.format("Dispatching submitting event (operation_id: %s)", submittingEvent.getOperationUid()));

          try (final CallableStatement changeOperationStateStatement = connection.prepareCall("call change_operation_state(?, ?, ?, ?)")) {
            changeOperationStateStatement.setString(1, submittingEvent.getOperationUid());
            changeOperationStateStatement.setInt(2, OperationState.SUBMITTED);
            changeOperationStateStatement.setInt(3, submittingEvent.getTotalMessages());
            changeOperationStateStatement.setTimestamp(4, submittingEvent.getTimestamp());

            changeOperationStateStatement.execute();

            submittedEventProcessingTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
          }
        } else if (event instanceof CancelledEvent) {
          final CancelledEvent cancelledEvent = (CancelledEvent) event;

          if (LOGGER.isDebugEnabled())
            LOGGER.debug(String.format("Dispatching cancelled event (operation_id: %s)", cancelledEvent.getOperationUid()));

          try (final CallableStatement changeOperationStateStatement = connection.prepareCall("call change_operation_state(?, ?, ?, ?)")) {
            changeOperationStateStatement.setString(1, cancelledEvent.getOperationUid());
            changeOperationStateStatement.setInt(2, OperationState.CANCELLED);
            changeOperationStateStatement.setNull(3, Types.INTEGER);
            changeOperationStateStatement.setTimestamp(4, cancelledEvent.getTimestamp());

            changeOperationStateStatement.execute();

            cancelledEventProcessingTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
          }
        } else if (event instanceof CancelledToReplaceEvent) {
          final CancelledToReplaceEvent cancelledToReplaceEvent = (CancelledToReplaceEvent) event;

          if (LOGGER.isDebugEnabled())
            LOGGER.debug(String.format("Dispatching cancelled to replace event (operation_id: %s)", cancelledToReplaceEvent.getOperationUid()));

          try (final CallableStatement changeOperationStateStatement = connection.prepareCall("call change_operation_state(?, ?, ?, ?)")) {
            changeOperationStateStatement.setString(1, cancelledToReplaceEvent.getOperationUid());
            changeOperationStateStatement.setInt(2, OperationState.CANCELLED_TO_REPLACE);
            changeOperationStateStatement.setNull(3, Types.INTEGER);
            changeOperationStateStatement.setTimestamp(4, cancelledToReplaceEvent.getTimestamp());

            changeOperationStateStatement.execute();

            cancelledToReplaceEventProcessingTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
          }
        } else if (event instanceof SubmittingShortMessageEvent) {
          final SubmittingShortMessageEvent submittingShortMessageEvent = (SubmittingShortMessageEvent) event;

          if (LOGGER.isDebugEnabled())
            LOGGER.debug(String.format("Dispatching submitting short message event (id: %s)", submittingShortMessageEvent.getId()));

          try (final CallableStatement changeShortMessageStateStatement = connection.prepareCall("call `short_message#on_submitting`(?, ?, ?, ?, ?)")) {
            changeShortMessageStateStatement.setLong(1, submittingShortMessageEvent.getId());
            changeShortMessageStateStatement.setString(2, submittingShortMessageEvent.getOperationId());
            changeShortMessageStateStatement.setInt(3, submittingShortMessageEvent.getSequence());
            changeShortMessageStateStatement.setInt(4, submittingShortMessageEvent.getEndPosition());
            changeShortMessageStateStatement.setTimestamp(5, submittingShortMessageEvent.getTimestamp());

            changeShortMessageStateStatement.execute();

            submittingShortMessageEventProcessingTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
          }
        } else if (event instanceof SubmittedShortMessageEvent) {
          final SubmittedShortMessageEvent submittedEvent = (SubmittedShortMessageEvent) event;

          if (LOGGER.isDebugEnabled())
            LOGGER.debug(String.format("Dispatching submitted short message event (id: %s, message_id: %s)", submittedEvent.getId(), submittedEvent.getMessageId()));

          try (final CallableStatement changeShortMessageStateStatement = connection.prepareCall("call `short_message#on_submitted`(?, ?, ?, ?)")) {
            changeShortMessageStateStatement.setLong(1, submittedEvent.getId());
            changeShortMessageStateStatement.setLong(2, submittedEvent.getMessageId());
            changeShortMessageStateStatement.setInt(3, submittedEvent.getSmppStatus());
            changeShortMessageStateStatement.setTimestamp(4, submittedEvent.getTimestamp());

            changeShortMessageStateStatement.execute();

            submittedShortMessageEventProcessingTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
          }
        } else if (event instanceof CancellingShortMessageEvent) {
          final CancellingShortMessageEvent cancellingShortMessageEvent = (CancellingShortMessageEvent) event;

          if (LOGGER.isDebugEnabled())
            LOGGER.debug(String.format("Dispatching cancelling short message event (id: %s)", cancellingShortMessageEvent.getId()));

          try (final CallableStatement changeShortMessageStateStatement = connection.prepareCall("call `short_message#on_cancelling`(?, ?)")) {
            changeShortMessageStateStatement.setLong(1, cancellingShortMessageEvent.getId());
            changeShortMessageStateStatement.setTimestamp(2, cancellingShortMessageEvent.getTimestamp());

            changeShortMessageStateStatement.execute();

            cancellingShortMessageEventProcessingTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
          }
        } else if (event instanceof CancelledShortMessageEvent) {
          final CancelledShortMessageEvent cancelledShortMessageEvent = (CancelledShortMessageEvent) event;

          if (LOGGER.isDebugEnabled())
            LOGGER.debug(String.format("Dispatching cancelled short message event (id: %s)", cancelledShortMessageEvent.getId()));

          try (final CallableStatement changeShortMessageStateStatement = connection.prepareCall("call `short_message#on_cancelled`(?, ?, ?)")) {
            changeShortMessageStateStatement.setLong(1, cancelledShortMessageEvent.getId());
            changeShortMessageStateStatement.setInt(2, cancelledShortMessageEvent.getSmppStatus());
            changeShortMessageStateStatement.setTimestamp(3, cancelledShortMessageEvent.getTimestamp());

            changeShortMessageStateStatement.execute();

            cancelledShortMessageEventProcessingTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
          }
        } else if (event instanceof DeliveredEvent) {
          final DeliveredEvent deliveredEvent = (DeliveredEvent) event;

          if (LOGGER.isDebugEnabled())
            LOGGER.debug(String.format("Dispatching delivered event (message_id: %s)", deliveredEvent.getMessageId()));

          try (final CallableStatement changeShortMessageStateStatement = connection.prepareCall("call `short_message#on_delivered`(?, ?, ?, ?)")) {
            changeShortMessageStateStatement.setInt(1, deliveredEvent.getMessageId());
            changeShortMessageStateStatement.setInt(2, deliveredEvent.getMessageState());
            changeShortMessageStateStatement.setTimestamp(3, deliveredEvent.getTimestamp());
            changeShortMessageStateStatement.setTimestamp(4, deliveredEvent.getDeliveryTimestamp());

            changeShortMessageStateStatement.execute();

            switch (deliveredEvent.getMessageState()) {
              case ShortMessageState.DELIVERED:
                deliveredShortMessageEventProcessingTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
                break;

              case ShortMessageState.EXPIRED:
                expiredEventProcessingTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
                break;

              case ShortMessageState.DELETED:
                deletedEventProcessingTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
                break;

              case ShortMessageState.UNDELIVERABLE:
                undeliveredEventProcessingTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
                break;

              case ShortMessageState.ACCEPTED:
                acceptedEventProcessingTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
                break;

              case ShortMessageState.REJECTED:
                rejectedEventProcessingTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
                break;

              case ShortMessageState.UNKNOWN:
                unknownEventProcessingTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
                break;
            }
          }
        }

        connection.commit();
        completed = true;
        executionTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
      } catch (final SQLException e) {
        try {
          if (connection != null) {
            connection.close();
          }
        } catch (Exception ignored) {
        }

        connection = null;

        executionTimeCounter.setValue(SoftTime.getTimestamp() - startTime);

        if (e.getErrorCode() == MySQLErrorCodes.ER_LOCK_DEADLOCK || e.getErrorCode() == MySQLErrorCodes.ER_LOCK_WAIT_TIMEOUT) {
          deadlockCounter++;
          this.deadlockCounter.incrementValue();

          if (deadlockCounter >= deadlockRetryAttempts) {
            LOGGER.warn(String.format("Cannot dispatch %s", event), e);
            completed = true;
          } else {
            Thread.sleep(deadlockRetryInterval);
            completed = false;
          }
        } else {
          LOGGER.warn(String.format("Cannot dispatch %s", event), e);
          completed = true;
        }
      }
    } while (!completed);

    eventProcessingTimeCounter.setValue(SoftTime.getTimestamp() - startTime);
  }

  @Override
  public final String toString() {
    return THREAD_NAME;
  }
}