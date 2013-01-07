package kg.dtg.smssender.statistic;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 5/25/11
 * Time: 11:19 PM
 */
public final class MinMaxCounterToken extends CounterToken {
  public MinMaxCounterToken(final String name, final String measureUnit) {
    super(name, measureUnit);

    statisticCollector.registerMinMaxCounter(this);
  }

  public final void setValue(final long value) throws InterruptedException {
    statisticCollector.registerOperation(new SetValueOperation(this, value));
  }
}
