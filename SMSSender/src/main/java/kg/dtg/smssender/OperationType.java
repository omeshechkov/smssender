package kg.dtg.smssender;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 7/17/11
 * Time: 5:20 PM
 */
public final class OperationType {
  public static final int SUBMIT_SHORT_MESSAGE = 0;
  public static final int REPLACE_SHORT_MESSAGE = 10;
  public static final int CANCEL_SHORT_MESSAGE = 20;
  public static final int SUBMIT_USSD = 30;

  private OperationType() throws IllegalAccessException {
    throw new IllegalAccessException();
  }
}
