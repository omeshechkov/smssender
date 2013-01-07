package kg.dtg.smssender.statistic;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 5/25/11
 * Time: 11:12 PM
 */
public final class MinMaxCounter extends Counter {
  private long maxValue;
  private long minValue;

  MinMaxCounter(final String name, final String measureUnit) {
    super(name, measureUnit);
    reset();
  }

  final void setValue(final long value) {
    if (value > maxValue) {
      maxValue = value;
    }

    if (value < minValue) {
      minValue = value;
    }
  }

  final long getMaxValue() {
    return maxValue;
  }

  final long getMinValue() {
    return minValue;
  }

  final void reset() {
    maxValue = Long.MIN_VALUE;
    minValue = Long.MAX_VALUE;
  }
}
