package kg.dtg.smssender.events;

import kg.dtg.smssender.Event;

import java.sql.Timestamp;

public class CancelledShortMessageEvent implements Event {
  private static final String EVENT_NAME = "Cancelled short message event";

  private final long id;
  private final int smppStatus;

  private final Timestamp timestamp = new Timestamp(System.currentTimeMillis());

  public CancelledShortMessageEvent(final long id, final int smppStatus) {
    this.id = id;
    this.smppStatus = smppStatus;
  }

  public final Timestamp getTimestamp() {
    return timestamp;
  }

  public final long getId() {
    return this.id;
  }

  public final int getSmppStatus() {
    return this.smppStatus;
  }

  @Override
  public final String toString() {
    return String.format("%s (id: %s, smpp_status: %s)", EVENT_NAME, id, smppStatus);
  }
}
