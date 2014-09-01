package kg.dtg.smssender.events;

import kg.dtg.smssender.Event;

import java.sql.Timestamp;

public class SubmittingShortMessageEvent implements Event {
  private static final String EVENT_NAME = "Submitting short message event";

  private final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
  private final long id;
  private final String operationId;
  private final int sequence;
  private final int endPosition;

  public SubmittingShortMessageEvent(final long id, final String operationId, final int sequence, final int endPosition) {
    this.id = id;
    this.operationId = operationId;
    this.sequence = sequence;
    this.endPosition = endPosition;
  }

  public final Timestamp getTimestamp() {
    return timestamp;
  }

  public final long getId() {
    return id;
  }

  public final String getOperationId() {
    return operationId;
  }

  public final int getSequence() {
    return sequence;
  }

  public final int getEndPosition() {
    return endPosition;
  }

  @Override
  public final String toString() {
    return String.format("%s (id: %s, operation_id: %s, sequence: %s, end_position: %s)",
            EVENT_NAME, id, operationId, sequence, endPosition);
  }
}