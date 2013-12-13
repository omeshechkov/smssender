package kg.dtg.smssender.Operations;

/**
 * Created with IntelliJ IDEA.
 * User: oleg
 * Date: 12/1/13
 * Time: 10:16 PM
 */
public final class SubmitShortMessageOperation extends SubmitOperation {
  public SubmitShortMessageOperation(final String uid,
                                     final String sourceNumber, final int sourceTon, final int sourceNpi,
                                     final String destinationNumber, final int destinationTon, final int destinationNpi,
                                     final String message, final String serviceType, final int state) {
    super(uid, sourceNumber, sourceTon, sourceNpi,
            destinationNumber, destinationTon, destinationNpi,
            message, serviceType, state);
  }
}
