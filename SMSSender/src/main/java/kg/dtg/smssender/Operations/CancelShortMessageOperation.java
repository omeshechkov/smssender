package kg.dtg.smssender.Operations;

/**
 * Created with IntelliJ IDEA.
 * User: oleg
 * Date: 12/1/13
 * Time: 10:22 PM
 */
public final class CancelShortMessageOperation extends Operation {
  private final int messageId;

  public CancelShortMessageOperation(final String uid, final String sourceNumber, final String destinationNumber,
                                     final String serviceType, final int state, final int messageId) {
    super(uid, sourceNumber, destinationNumber, serviceType, state);


    this.messageId = messageId;
  }

  public final int getMessageId() {
    return messageId;
  }
}
