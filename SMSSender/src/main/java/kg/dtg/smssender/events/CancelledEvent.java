package kg.dtg.smssender.events;

import kg.dtg.smssender.Event;
import kg.dtg.smssender.Operations.CancelShortMessageOperation;

import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * User: oleg
 * Date: 12/1/13
 * Time: 11:14 PM
 */
public class CancelledEvent implements Event {
  private static final String EVENT_NAME = "Cancelled event";

  private final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
  private final CancelShortMessageOperation operation;
  private final int smppStatus;

  public CancelledEvent(final CancelShortMessageOperation operation, final int smppStatus) {
    this.operation = operation;
    this.smppStatus = smppStatus;
  }

  public final Timestamp getTimestamp() {
    return timestamp;
  }

  public final CancelShortMessageOperation getOperation() {
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