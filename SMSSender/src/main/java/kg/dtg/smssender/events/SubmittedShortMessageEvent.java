package kg.dtg.smssender.events;

import kg.dtg.smssender.Event;

import java.sql.Timestamp;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 4/10/11
 * Time: 7:38 PM
 */
public final class SubmittedShortMessageEvent implements Event {
  private static final String EVENT_NAME = "Submitted short message event";

  private final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
  private final long id;
  private final Integer messageId;
  private final int smppStatus;

  public SubmittedShortMessageEvent(final long id, final Integer messageId, final int smppStatus) {
    this.id = id;
    this.messageId = messageId;
    this.smppStatus = smppStatus;
  }

  public final Timestamp getTimestamp() {
    return timestamp;
  }

  public final long getId() {
    return id;
  }

  public final Integer getMessageId() {
    return messageId;
  }

  public final int getSmppStatus() {
    return smppStatus;
  }

  @Override
  public final String toString() {
    return String.format("%s (id: %s, message id: %s, smpp status: %s)", EVENT_NAME, id, messageId, smppStatus);
  }
}