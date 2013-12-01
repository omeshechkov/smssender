package kg.dtg.smssender.events;

import kg.dtg.smssender.Event;
import kg.dtg.smssender.Operations.CancelShortMessageOperation;

import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * User: oleg
 * Date: 12/1/13
 * Time: 11:15 PM
 */
public class CancellingEvent implements Event {
  private static final String EVENT_NAME = "Cancelling event";

  private final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
  private final CancelShortMessageOperation operation;

  public CancellingEvent(final CancelShortMessageOperation operation) {
    this.operation = operation;
  }

  public final Timestamp getTimestamp() {
    return timestamp;
  }

  public final CancelShortMessageOperation getOperation() {
    return operation;
  }

  @Override
  public final String toString() {
    return String.format("%s (operation id: %s)", EVENT_NAME, operation.getUid());
  }
}