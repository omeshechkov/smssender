package kg.dtg.smssender;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 7/17/11
 * Time: 5:20 PM
 */
public final class MessageState {
  public static final int SCHEDULED_STATE = 0;
  public static final int SUBMITED_STATE = 1;
  public static final int DELIVERED_STATE = 2;
  public static final int REPLACE_STATE = 3;

  private MessageState() throws IllegalAccessException {
    throw new IllegalAccessException();
  }
}
