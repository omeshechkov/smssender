package kg.dtg.smssender.Operations;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 5/24/11
 * Time: 12:52 AM
 */
public abstract class Message {
  public static final int SHORT_MESSAGE = 0;
  public static final int USSD = 1;

  private final String uid;

  private final String sourceNumber;
  private final String destinationNumber;
  private final String message;
  private final int messageType;

  public Message(final String uid, final String sourceNumber, final String destinationNumber, final String message, final int messageType) {
    this.uid = uid;
    this.sourceNumber = sourceNumber;
    this.destinationNumber = destinationNumber;
    this.message = message;
    this.messageType = messageType;
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

  public int getMessageType() {
    return messageType;
  }
}