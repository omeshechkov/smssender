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
  private final int sourceTon;
  private final int sourceNpi;

  private final String destinationNumber;
  private final int destinationTon;
  private final int destinationNpi;

  private final String serviceType;
  private final int state;

  public Operation(final String uid,
                   final String sourceNumber, final int sourceTon, final int sourceNpi,
                   final String destinationNumber, final int destinationTon, final int destinationNpi,
                   final String serviceType, final int state) {
    this.uid = uid;

    this.sourceNumber = sourceNumber;
    this.sourceTon = sourceTon;
    this.sourceNpi = sourceNpi;

    this.destinationNumber = destinationNumber;
    this.destinationTon = destinationTon;
    this.destinationNpi = destinationNpi;

    this.serviceType = serviceType;
    this.state = state;
  }

  public final String getUid() {
    return uid;
  }

  public final String getSourceNumber() {
    return sourceNumber;
  }

  public final int getSourceTon() {
    return sourceTon;
  }

  public final int getSourceNpi() {
    return sourceNpi;
  }

  public final String getDestinationNumber() {
    return destinationNumber;
  }

  public final int getDestinationTon() {
    return destinationTon;
  }

  public final int getDestinationNpi() {
    return destinationNpi;
  }

  public final String getServiceType() {
    return serviceType;
  }

  public final int getState() {
    return state;
  }
}