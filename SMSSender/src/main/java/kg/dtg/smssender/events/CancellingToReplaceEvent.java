package kg.dtg.smssender.events;

import kg.dtg.smssender.Event;
import kg.dtg.smssender.Operations.ReplaceShortMessageOperation;

import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * User: oleg
 * Date: 12/1/13
 * Time: 10:42 PM
 */
public class CancellingToReplaceEvent implements Event {
  private static final String EVENT_NAME = "Cancelling to replace event";

  private final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
  private final ReplaceShortMessageOperation operation;

  public CancellingToReplaceEvent(final ReplaceShortMessageOperation operation) {
    this.operation = operation;
  }

  public final Timestamp getTimestamp() {
    return timestamp;
  }

  public final ReplaceShortMessageOperation getOperation() {
    return operation;
  }

  @Override
  public final String toString() {
    return String.format("%s (operation id: %s)", EVENT_NAME, operation.getUid());
  }
}
