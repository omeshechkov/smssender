package kg.dtg.smssender.events;

import kg.dtg.smssender.Event;

import java.sql.Timestamp;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 5/25/11
 * Time: 11:51 PM
 */
public final class ReplacedEvent implements Event {
  private static final String EVENT_NAME = "Replaced event";

  private final Integer messageId;
  private final String message;

  public ReplacedEvent(final Integer messageId, final String message) {
    this.messageId = messageId;
    this.message = message;
  }

  public final Integer getMessageId() {
    return messageId;
  }

  public final String getMessage() {
    return message;
  }

  @Override
  public final String toString() {
    return String.format("%s (message id: %s)", EVENT_NAME, messageId);
  }
}
