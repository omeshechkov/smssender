package kg.dtg.smssender.Operations;

/**
 * Created with IntelliJ IDEA.
 * User: oleg
 * Date: 12/1/13
 * Time: 10:16 PM
 */
public final class SubmitShortMessageOperation extends SubmitOperation {
  public SubmitShortMessageOperation(final String uid, final String sourceNumber, final String destinationNumber,
                                     final String message, final String serviceType, final int state) {
    super(uid, sourceNumber, destinationNumber, message, serviceType, state);
  }
}
