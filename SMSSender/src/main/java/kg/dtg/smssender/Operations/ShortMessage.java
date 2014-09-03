package kg.dtg.smssender.Operations;

public final class ShortMessage {
  private final long id;

  private final int messageId;

  public ShortMessage(final long id, final int messageId) {
    this.id = id;
    this.messageId = messageId;
  }

  public final long getId() {
    return id;
  }

  public final int getMessageId() {
    return messageId;
  }
}
