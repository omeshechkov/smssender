package kg.dtg.smssender.events;

import kg.dtg.smssender.Event;
import kg.dtg.smssender.Operations.Operation;
import kg.dtg.smssender.Operations.SubmitOperation;

import java.sql.Timestamp;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 4/10/11
 * Time: 7:38 PM
 */
public final class SubmittedEvent implements Event {
  private static final String EVENT_NAME = "Submitted event";

  private final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
  private final Operation operation;
  private final Integer messageId;
  private final int smppStatus;

  public SubmittedEvent(final Operation operation, final Integer messageId, final int smppStatus) {
    this.operation = operation;
    this.messageId = messageId;
    this.smppStatus = smppStatus;
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

  public final int getSmppStatus() {
    return smppStatus;
  }

  @Override
  public final String toString() {
    return String.format("%s (operation id: %s, message id: %s)", EVENT_NAME, operation.getUid(), messageId);
  }
}