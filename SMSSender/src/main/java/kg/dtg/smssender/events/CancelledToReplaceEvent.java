package kg.dtg.smssender.events;

import kg.dtg.smssender.Event;
import kg.dtg.smssender.Operations.ReplaceShortMessageOperation;

import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * User: oleg
 * Date: 12/1/13
 * Time: 10:43 PM
 */
public class CancelledToReplaceEvent implements Event {
  private static final String EVENT_NAME = "Cancelled to replace event";

  private final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
  private final ReplaceShortMessageOperation operation;
  private final int smppStatus;

  public CancelledToReplaceEvent(final ReplaceShortMessageOperation operation, final int smppStatus) {
    this.operation = operation;
    this.smppStatus = smppStatus;
  }

  public final Timestamp getTimestamp() {
    return timestamp;
  }

  public final ReplaceShortMessageOperation getOperation() {
    return operation;
  }

  public final int getSmppStatus() {
    return smppStatus;
  }

  @Override
  public final String toString() {
    return String.format("%s (operation id: %s)", EVENT_NAME, operation.getUid());
  }
}
