package kg.dtg.smssender.statistic;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 5/25/11
 * Time: 9:24 PM
 */
final class SnapshotPeriod {
  public final int period;

  SnapshotPeriod(int period) {
    this.period = period;
  }

  @Override
  public final String toString() {
    if (period == 0) {
      return "Total";
    }

    int period = this.period;

    int days = 0;
    int hours = 0;
    int minutes = 0;

    if (period >= 86400) {
      days = period / 86400;
      period -= days * 86400;
    }

    if (period >= 3600) {
      hours = period / 3600;
      period -= hours * 3600;
    }

    if (period >= 60) {
      minutes = period / 60;
      period -= minutes * 60;
    }

    final StringBuilder sb = new StringBuilder();

    if (days > 0) {
      sb.append(days).append(" day(s) ");
    }

    if (hours > 0) {
      sb.append(hours).append(" hour(s) ");
    }

    if (minutes > 0) {
      sb.append(minutes).append(" minute(s) ");
    }

    if (period > 0) {
      sb.append(period).append(" seconds");
    }

    return sb.toString();
  }
}
