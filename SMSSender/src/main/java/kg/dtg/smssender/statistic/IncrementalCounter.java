package kg.dtg.smssender.statistic;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 5/25/11
 * Time: 11:12 PM
 */
public final class IncrementalCounter extends Counter {
  private int value;

  public IncrementalCounter(final String name, final String measureUnit) {
    super(name, measureUnit);
    reset();
  }

  final void increment() {
    value++;
  }

  public final int getValue() {
    return value;
  }

  @Override
  final void reset() {
    value = 0;
  }
}
