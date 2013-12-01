package kg.dtg.smssender.events;

import kg.dtg.smssender.Event;

import java.sql.Timestamp;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 4/22/11
 * Time: 1:39 AM
 */
public final class MessageReceivedEvent implements Event {
  private static final String EVENT_NAME = "Received event";

  private final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
  private final String sourceNumber;
  private final String destinationNumber;
  private final String message;

  public MessageReceivedEvent(final String sourceNumber, final String destinationNumber, final String message) {
    this.sourceNumber = sourceNumber;
    this.destinationNumber = destinationNumber;
    this.message = message;
  }

  public final Timestamp getTimestamp() {
    return timestamp;
  }

  public final String getSourceNumber() {
    return sourceNumber;
  }

  public final String getDestinationNumber() {
    return destinationNumber;
  }

  public final String getMessage() {
    return message;
  }

  @Override
  public final String toString() {
    return String.format("%s (source number: %s, destination number: %s)", EVENT_NAME, sourceNumber, destinationNumber);
  }
}
