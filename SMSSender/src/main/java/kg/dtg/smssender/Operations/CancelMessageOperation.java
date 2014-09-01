package kg.dtg.smssender.Operations;

/**
 * Created with IntelliJ IDEA.
 * User: oleg
 * Date: 12/1/13
 * Time: 10:22 PM
 */
public final class CancelMessageOperation extends Operation {
  private final long currentSequence;
  private final ShortMessage[] shortMessages;

  public CancelMessageOperation(final String uid,
                                final String sourceNumber, final int sourceTon, final int sourceNpi,
                                final String destinationNumber, final int destinationTon, final int destinationNpi,
                                final String serviceType, final int state, final ShortMessage[] shortMessages, final long currentSequence) {
    super(uid, sourceNumber, sourceTon, sourceNpi,
            destinationNumber, destinationTon, destinationNpi,
            serviceType, state);


    this.shortMessages = shortMessages;
    this.currentSequence = currentSequence;
  }

  public final ShortMessage[] getShortMessages() {
    return shortMessages;
  }

  public final long getCurrentSequence() {
    return currentSequence;
  }}