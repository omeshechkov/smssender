package kg.dtg.smssender.statistic;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 5/25/11
 * Time: 11:20 PM
 */
public final class IncrementalCounterToken extends CounterToken {
  public IncrementalCounterToken(final String name, final String measureUnit) {
    super(name, measureUnit);

    statisticCollector.registerIncrementalCounter(this);
  }

  public final void incrementValue() throws InterruptedException {
    statisticCollector.registerOperation(new IncrementValueOperation(this));
  }
}
