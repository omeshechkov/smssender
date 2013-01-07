package kg.dtg.smssender.events;

import kg.dtg.smssender.Event;
import kg.dtg.smssender.Session;

import java.sql.Timestamp;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 4/10/11
 * Time: 7:38 PM
 */
public final class SubmitedEvent implements Event {
  private static final String EVENT_NAME = "Submited event";

  private final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
  private final Session session;
  private final Integer messageId;
  private final String message;

  public SubmitedEvent(final Session session, final Integer messageId, final String message) {
    this.session = session;
    this.messageId = messageId;
    this.message = message;
  }

  public final Timestamp getTimestamp() {
    return timestamp;
  }

  public final Integer getMessageId() {
    return messageId;
  }

  public final Session getSession() {
    return session;
  }

  public final String getMessage() {
    return message;
  }

  @Override
  public final String toString() {
    return String.format("%s (session id: %s, message id: %s)", EVENT_NAME, session.getUid(), messageId);
  }
}