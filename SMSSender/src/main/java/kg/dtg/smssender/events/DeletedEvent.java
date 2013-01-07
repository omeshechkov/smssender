package kg.dtg.smssender.events;

import kg.dtg.smssender.Event;

import java.sql.Timestamp;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 21.09.11
 * Time: 14:01
 */
public final class DeletedEvent implements Event {
  private static final String EVENT_NAME = "Deleted event";

  private final Timestamp timestamp;
  private final int messageId;

  public DeletedEvent(final int messageId, final Timestamp timestamp) {
    this.messageId = messageId;
    this.timestamp = timestamp;
  }

  public final Timestamp getTimestamp() {
    return timestamp;
  }

  public final int getMessageId() {
    return messageId;
  }

  @Override
  public final String toString() {
    return String.format("%s (message id: %s, timestamp: %s)", EVENT_NAME, messageId, timestamp);
  }
}