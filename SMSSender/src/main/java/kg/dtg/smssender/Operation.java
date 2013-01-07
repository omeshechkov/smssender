package kg.dtg.smssender;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 5/24/11
 * Time: 12:52 AM
 */
public final class Operation {
  private final Session session;

  private final String sourceNumber;
  private final String destinationNumber;
  private final String message;
  private final int state;

  public Operation(final Session session, final String sourceNumber, final String destinationNumber, final String message, final int state) {
    this.session = session;
    this.sourceNumber = sourceNumber;
    this.destinationNumber = destinationNumber;
    this.message = message;
    this.state = state;
  }

  public final Session getSession() {
    return session;
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

  public final int getState() {
    return state;
  }
}
