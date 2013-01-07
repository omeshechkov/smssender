package kg.dtg.smssender.statistic;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 5/25/11
 * Time: 8:37 PM
 */
public abstract class Counter {
  private final String name;
  private final String measureUnit;

  Counter(final String name, final String measureUnit) {
    this.name = name;
    this.measureUnit = measureUnit;
    reset();
  }

  abstract void reset();

  final String getName() {
    return name;
  }

  final String getMeasureUnit() {
    return measureUnit;
  }
}