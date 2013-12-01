package kg.dtg.smssender.Operations;

/**
 * Created with IntelliJ IDEA.
 * User: oleg
 * Date: 12/1/13
 * Time: 11:10 PM
 */
public final class OperationState {
  public static final int SCHEDULED = 0;
  public static final int SUBMITTING = 1;
  public static final int SUBMITTED = 2;
  public static final int CANCELLING = 3;
  public static final int CANCELLED = 4;
  public static final int CANCELLING_TO_REPLACE = 5;
  public static final int CANCELLED_TO_REPLACE = 6;
  public static final int DELIVERED = 7;
  public static final int EXPIRED = 8;
  public static final int DELETED = 9;
  public static final int UNDELIVERABLE = 10;
  public static final int ACCEPTED = 11;
  public static final int REJECTED = 12;
  public static final int UNKNOWN = 13;

  private OperationState() throws IllegalAccessException {
    throw new IllegalAccessException();
  }
}
