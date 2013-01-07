package kg.dtg.smssender.statistic;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 5/25/11
 * Time: 11:18 PM
 */
class Operation {
  private final CounterToken counterToken;

  Operation(final CounterToken counterToken) {
    this.counterToken = counterToken;
  }

  public final CounterToken getCounterToken() {
    return counterToken;
  }
}
