package kg.dtg.smssender.Operations;

/**
 * Created with IntelliJ IDEA.
 * User: oleg
 * Date: 12/1/13
 * Time: 11:10 PM
 */
public final class ShortMessageState {
  public static final int SUBMITING = 1;
  public static final int SUBMITED = 2;
  public static final int CANCELLING = 3;
  public static final int CANCELED = 4;
  public static final int DELIVERED = 7;
  public static final int EXPIRED = 8;
  public static final int DELETED = 9;
  public static final int UNDELIVERABLE = 10;
  public static final int ACCEPTED = 11;
  public static final int REJECTED = 12;
  public static final int UNKNOWN = 13;

  private ShortMessageState() throws IllegalAccessException {
    throw new IllegalAccessException();
  }
}
