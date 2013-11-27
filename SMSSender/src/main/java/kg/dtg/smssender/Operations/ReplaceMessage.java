package kg.dtg.smssender.Operations;

/**
 * Created with IntelliJ IDEA.
 * User: oleg
 * Date: 3/31/13
 * Time: 3:13 PM
 */
public final class ReplaceMessage extends Message {
  public ReplaceMessage(final String uid, final String sourceNumber, final String destinationNumber, final String message, final int messageType) {
    super(uid, sourceNumber, destinationNumber, message, messageType);
  }
}