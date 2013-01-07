package kg.dtg.smssender;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 7/17/11
 * Time: 5:42 PM
 */
public final class ShortMessage {
  private int messageId;
  private String message;

  public ShortMessage(final String message) {
    this.message = message;
  }

  public ShortMessage(final int messageId) {
    this.messageId = messageId;
  }

  public final int getMessageId() {
    return messageId;
  }

  public final void setMessage(final String message) {
    this.message = message;
  }

  public final String getMessage() {
    return message;
  }
}