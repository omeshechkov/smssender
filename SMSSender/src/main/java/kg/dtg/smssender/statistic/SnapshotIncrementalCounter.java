package kg.dtg.smssender.statistic;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 5/25/11
 * Time: 11:29 PM
 */
final class SnapshotIncrementalCounter extends SnapshotCounter {
  private final long value;

  SnapshotIncrementalCounter(final String name, final String measureUnit, final long value) {
    super(name, measureUnit);
    this.value = value;
  }

  public final long getValue() {
    return value;
  }
}