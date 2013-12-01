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

  private final String serviceType;
  private final int state;

  public Operation(final String uid, final String sourceNumber, final String destinationNumber, final String serviceType,
                   final int state) {
    this.uid = uid;
    this.sourceNumber = sourceNumber;
    this.destinationNumber = destinationNumber;
    this.serviceType = serviceType;
    this.state = state;
  }

  public final String getUid() {
    return uid;
  }

  public final String getSourceNumber() {
    return sourceNumber;
  }

  public final String getDestinationNumber() {
    return destinationNumber;
  }

  public final String getServiceType() {
    return serviceType;
  }

  public final int getState() {
    return state;
  }
}