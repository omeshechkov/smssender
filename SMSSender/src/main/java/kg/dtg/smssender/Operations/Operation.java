package kg.dtg.smssender.Operations;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 5/24/11
 * Time: 12:52 AM
 */
public abstract class Operation {
  private final String uid;

  private final String sourceNumber;
  private final String destinationNumber;
  private final String message;

  public Operation(final String uid, final String sourceNumber, final String destinationNumber, final String message) {
    this.uid = uid;
    this.sourceNumber = sourceNumber;
    this.destinationNumber = destinationNumber;
    this.message = message;
  }

  public final String getId() {
    return uid;
  }

  public final String getSourceNumber() {
    return sourceNumber;
  }

  public final String getDestinationNumber() {
    return destinationNumber;
  }

  public final String getMessage() {
    return message;
  }
}
