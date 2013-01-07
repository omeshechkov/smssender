package kg.dtg.smssender.statistic;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 5/25/11
 * Time: 8:44 PM
 */
public abstract class CounterToken {
  protected final StatisticCollector statisticCollector;
  final String name;
  final String measureUnit;

  CounterToken(final String name, final String measureUnit) {
    this.statisticCollector = StatisticCollector.instance;
    this.name = name;
    this.measureUnit = measureUnit;
  }

  public final String getName() {
    return name;
  }

  public final String getMeasureUnit() {
    return measureUnit;
  }
}
