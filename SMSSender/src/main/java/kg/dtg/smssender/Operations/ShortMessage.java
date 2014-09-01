package kg.dtg.smssender.Operations;

public final class ShortMessage {
  private final long id;

  private final int smppState;

  private final int messageId;

  public ShortMessage(final long id, final int smppState, final int messageId) {
    this.id = id;
    this.smppState = smppState;
    this.messageId = messageId;
  }

  public final long getId() {
    return id;
  }

  public final int getSmppState() {
    return smppState;
  }

  public final int getMessageId() {
    return messageId;
  }
}
