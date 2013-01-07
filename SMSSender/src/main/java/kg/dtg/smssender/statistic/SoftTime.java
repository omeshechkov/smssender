package kg.dtg.smssender.statistic;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 5/25/11
 * Time: 9:13 PM
 */
public final class SoftTime implements Runnable {
  private static SoftTime softTime;
  private static long timestamp = System.currentTimeMillis();

  static {
    softTime = new SoftTime();
  }

  public static long getTimestamp() {
    return timestamp;
  }

  public SoftTime() {
    new Thread(this).start();
  }

  @Override
  public void run() {
    for (; ; ) {
      timestamp = System.currentTimeMillis();

      try {
        Thread.sleep(20);
      } catch (InterruptedException ignored) {
        break;
      }
    }
  }
}
