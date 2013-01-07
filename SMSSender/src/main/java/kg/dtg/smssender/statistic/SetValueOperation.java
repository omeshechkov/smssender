package kg.dtg.smssender.statistic;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 5/25/11
 * Time: 8:58 PM
 */
final class SetValueOperation extends Operation {
  private final long value;

  SetValueOperation(CounterToken counterToken, long value) {
    super(counterToken);
    this.value = value;
  }

  public final long getValue() {
    return value;
  }
}
