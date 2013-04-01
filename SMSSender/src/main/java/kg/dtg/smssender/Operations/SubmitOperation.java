package kg.dtg.smssender.Operations;

/**
 * Created with IntelliJ IDEA.
 * User: oleg
 * Date: 3/31/13
 * Time: 3:12 PM
 */
public final class SubmitOperation extends Operation {
  public static final int SHORT_MESSAGE = 0;
  public static final int USSD = 1;

  private final int messageType;

  public SubmitOperation(final String uid, final String sourceNumber, final String destinationNumber, final String message, final int messageType) {
    super(uid, sourceNumber, destinationNumber, message);

    this.messageType = messageType;
  }

  public int getMessageType() {
    return messageType;
  }
}
