package kg.dtg.smssender.events;

import kg.dtg.smssender.Event;

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
  private final String operationUid;

  public CancelledToReplaceEvent(final String operationUid) {
    this.operationUid = operationUid;
  }

  public final Timestamp getTimestamp() {
    return timestamp;
  }

  public final String getOperationUid() {
    return operationUid;
  }

  @Override
  public final String toString() {
    return String.format("%s (operation id: %s)", EVENT_NAME, operationUid);
  }
}
