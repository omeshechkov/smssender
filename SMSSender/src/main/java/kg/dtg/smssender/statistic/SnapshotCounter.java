package kg.dtg.smssender.statistic;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 5/25/11
 * Time: 9:24 PM
 */
class SnapshotCounter implements Comparable<SnapshotCounter> {
  private final String name;
  private final String measureUnit;

  SnapshotCounter(final String name, final String measureUnit) {
    this.name = name;
    this.measureUnit = measureUnit;
  }

  public final String getName() {
    return name;
  }

  public final String getMeasureUnit() {
    return measureUnit;
  }

  @Override
  public final int compareTo(final SnapshotCounter that) {
    if (that == null) {
      throw new NullPointerException("that");
    }

    final int nameLength = name.length();

    final String thatName = that.name;
    final int thatNameLength = thatName.length();

    final int minLength = nameLength < thatNameLength ? nameLength : thatNameLength;

    final char[] nameChars = name.toCharArray();
    final char[] thatChars = thatName.toCharArray();

    for (int i = 0; i < minLength; i++) {
      final char ownChar = nameChars[i];
      final char thatChar = thatChars[i];

      if (ownChar < thatChar) {
        return -1;
      } else if (ownChar > thatChar) {
        return 1;
      }
    }

    return 0;
  }
}
