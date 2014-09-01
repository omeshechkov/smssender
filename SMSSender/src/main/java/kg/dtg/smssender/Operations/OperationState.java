package kg.dtg.smssender.Operations;

/**
 * Created with IntelliJ IDEA.
 * User: oleg
 * Date: 12/1/13
 * Time: 11:10 PM
 */
public final class OperationState {
  public static final int SCHEDULED = 0;
  public static final int SUBMITTED = 2;
  public static final int CANCELLED = 4;
  public static final int CANCELLED_TO_REPLACE = 6;
  public static final int DELIVERED = 7;

  private OperationState() throws IllegalAccessException {
    throw new IllegalAccessException();
  }
}