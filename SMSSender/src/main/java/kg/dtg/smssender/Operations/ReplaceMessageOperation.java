package kg.dtg.smssender.Operations;

/**
 * Created with IntelliJ IDEA.
 * User: oleg
 * Date: 12/1/13
 * Time: 10:17 PM
 */
public final class ReplaceMessageOperation extends Operation {
  private final String message;
  private final int currentSequence;
  private final ShortMessage[] shortMessages;

  public ReplaceMessageOperation(final String uid,
                                 final String sourceNumber, final int sourceTon, final int sourceNpi,
                                 final String destinationNumber, final int destinationTon, final int destinationNpi,
                                 final String serviceType, final int state, final ShortMessage[] shortMessages, final int currentSequence,
                                 final String message) {
    super(uid, sourceNumber, sourceTon, sourceNpi,
            destinationNumber, destinationTon, destinationNpi,
            serviceType, state);

    this.message = message;
    this.shortMessages = shortMessages;
    this.currentSequence = currentSequence;
  }

  public final String getMessage() {
    return message;
  }

  public final ShortMessage[] getShortMessages() {
    return shortMessages;
  }

  public final int getCurrentSequence() {
    return currentSequence;
  }
}