package kg.dtg.smssender.Operations;

/**
 * Created with IntelliJ IDEA.
 * User: oleg
 * Date: 3/31/13
 * Time: 3:12 PM
 */
public final class SubmitOperation extends Operation {
  public SubmitOperation(String uid, String sourceNumber, String destinationNumber, String message) {
    super(uid, sourceNumber, destinationNumber, message);
  }
}
