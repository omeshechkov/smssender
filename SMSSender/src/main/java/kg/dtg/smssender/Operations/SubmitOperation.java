package kg.dtg.smssender.Operations;

/**
 * Created with IntelliJ IDEA.
 * User: oleg
 * Date: 3/31/13
 * Time: 3:12 PM
 */
public abstract class SubmitOperation extends Operation {
  private final String message;

  public SubmitOperation(final String uid, final String sourceNumber, final String destinationNumber,
                         final String message, final String serviceType, final int state) {
    super(uid, sourceNumber, destinationNumber, serviceType, state);

    this.message = message;
  }

  public final String getMessage() {
    return message;
  }
}