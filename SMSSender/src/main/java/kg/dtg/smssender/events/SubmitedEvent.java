package kg.dtg.smssender.events;

import kg.dtg.smssender.Event;
import kg.dtg.smssender.Operations.Operation;

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
  private final Operation operation;
  private final Integer messageId;
  private final String message;

  public SubmitedEvent(final Operation operation, final Integer messageId, final String message) {
    this.operation = operation;
    this.messageId = messageId;
    this.message = message;
  }

  public final Timestamp getTimestamp() {
    return timestamp;
  }

  public final Integer getMessageId() {
    return messageId;
  }

  public final Operation getOperation() {
    return operation;
  }

  public final String getMessage() {
    return message;
  }

  @Override
  public final String toString() {
    return String.format("%s (operation id: %s, message id: %s)", EVENT_NAME, operation.getId(), messageId);
  }
}