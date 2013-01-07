package kg.dtg.smssender.statistic;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 5/25/11
 * Time: 8:39 PM
 */
public final class Period {
  public static final int UNLIMITED = 0;

  private final int period;
  private long timestamp = SoftTime.getTimestamp();

  Period(final int seconds) {
    this.period = seconds * 1000;
  }

  public final boolean isElapsed() {
    if (period == UNLIMITED) {
      return false;
    }

    final long currentTime = SoftTime.getTimestamp();

    if (currentTime - timestamp > period) {
      timestamp = currentTime;
      return true;
    }

    return false;
  }
}
