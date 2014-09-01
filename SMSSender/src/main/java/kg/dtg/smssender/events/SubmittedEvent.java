package kg.dtg.smssender.events;

import kg.dtg.smssender.Event;

import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * User: oleg
 * Date: 12/1/13
 * Time: 10:39 PM
 */
public class SubmittedEvent implements Event {
  private static final String EVENT_NAME = "Submitted event";

  private final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
  private final String operationUid;
  private final int totalMessages;

  public SubmittedEvent(final String operationUid, final int totalMessages) {
    this.operationUid = operationUid;
    this.totalMessages = totalMessages;
  }

  public final Timestamp getTimestamp() {
    return timestamp;
  }

  public final String getOperationUid() {
    return operationUid;
  }

  public final int getTotalMessages() {
    return totalMessages;
  }

  @Override
  public final String toString() {
    return String.format("%s (operation id: %s)", EVENT_NAME, operationUid);
  }
}
