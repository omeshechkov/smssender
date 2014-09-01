package kg.dtg.smssender.events;

import kg.dtg.smssender.Event;

import java.sql.Timestamp;

public class CancellingShortMessageEvent implements Event {
  private static final String EVENT_NAME = "Cancelling short message event";

  private final long id;

  private final Timestamp timestamp = new Timestamp(System.currentTimeMillis());

  public CancellingShortMessageEvent(final long id) {
    this.id = id;
  }

  public final Timestamp getTimestamp() {
    return timestamp;
  }

  public final long getId() {
    return this.id;
  }

  @Override
  public final String toString() {
    return String.format("%s (id: %s)", EVENT_NAME, id);
  }
}
