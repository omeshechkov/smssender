package kg.dtg.smssender.events;

import kg.dtg.smssender.Event;
import kg.dtg.smssender.Operations.Operation;

import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * User: oleg
 * Date: 12/1/13
 * Time: 10:39 PM
 */
public class SubmittingEvent implements Event {
  private static final String EVENT_NAME = "Submitting event";

  private final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
  private final Operation operation;

  public SubmittingEvent(final Operation operation) {
    this.operation = operation;
  }

  public final Timestamp getTimestamp() {
    return timestamp;
  }

  public final Operation getOperation() {
    return operation;
  }

  @Override
  public final String toString() {
    return String.format("%s (operation id: %s)", EVENT_NAME, operation.getUid());
  }
}
