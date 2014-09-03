package kg.dtg.smssender.events;

import kg.dtg.smssender.Event;

import java.sql.Timestamp;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 4/10/11
 * Time: 7:43 PM
 */
public final class DeliveredEvent implements Event {
  private static final String EVENT_NAME = "Delivered event";

  private final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
  private final Timestamp deliveryTimestamp;
  private final int messageId;
  private final int messageState;

  public DeliveredEvent(final int messageId, final int messageState, final Timestamp deliveryTimestamp) {
    this.messageId = messageId;
    this.deliveryTimestamp = deliveryTimestamp;
    this.messageState = messageState;
  }

  public final Timestamp getDeliveryTimestamp() {
    return deliveryTimestamp;
  }

  public final Timestamp getTimestamp() {
    return timestamp;
  }

  public final int getMessageId() {
    return messageId;
  }

  public final int getMessageState() {
    return messageState;
  }

  @Override
  public final String toString() {
    return String.format("%s (message_id: %s, message_state: %s, timestamp: %s, delivery_timestamp: %s)",
            EVENT_NAME, messageId, messageState, timestamp, deliveryTimestamp);
  }
}