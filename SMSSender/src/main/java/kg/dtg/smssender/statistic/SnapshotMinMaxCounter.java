package kg.dtg.smssender.statistic;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 5/25/11
 * Time: 11:28 PM
 */
final class SnapshotMinMaxCounter extends SnapshotCounter {
  private final long minValue;
  private final long maxValue;

  SnapshotMinMaxCounter(final String name, final String measureUnit, final long minValue, final long maxValue) {
    super(name, measureUnit);
    this.minValue = minValue;
    this.maxValue = maxValue;
  }

  public final String getMinValue() {
    return minValue == Long.MAX_VALUE ? "N/A" : Long.toString(minValue);
  }

  public final String getMaxValue() {
    return maxValue == Long.MIN_VALUE ? "N/A" : Long.toString(maxValue);
  }
}
