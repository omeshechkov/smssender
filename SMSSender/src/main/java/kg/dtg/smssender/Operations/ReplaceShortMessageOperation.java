package kg.dtg.smssender.Operations;

/**
 * Created with IntelliJ IDEA.
 * User: oleg
 * Date: 12/1/13
 * Time: 10:17 PM
 */
public final class ReplaceShortMessageOperation extends Operation {
  private final String message;
  private final int messageId;

  public ReplaceShortMessageOperation(final String uid, final String sourceNumber, final String destinationNumber,
                                      final String serviceType, final int state, final int messageId,
                                      final String message) {
    super(uid, sourceNumber, destinationNumber, serviceType, state);

    this.message = message;
    this.messageId = messageId;
  }

  public final String getMessage() {
    return message;
  }

  public final int getMessageId() {
    return messageId;
  }
}