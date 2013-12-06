package kg.dtg.smssender;

import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 4/8/11
 * Time: 2:38 PM
 */
public abstract class Dispatcher implements Runnable {
  private static Logger LOGGER = Logger.getLogger(Dispatcher.class);

  protected DispatcherState state = DispatcherState.RUNNING;

  public Dispatcher() {
    final Thread thread = new Thread(this);
    thread.setName(toString());
    thread.start();
  }

  public void setState(final DispatcherState state) {
    this.state = state;
  }

  public void run() {
    LOGGER.info(String.format("%s started...", this));

    try {
      //noinspection InfiniteLoopStatement
      for (; ;) {
        if (state != DispatcherState.RUNNING) {
          Thread.sleep(1);
          continue;
        }

        work();
        Thread.sleep(1);
      }
    } catch (InterruptedException ignored) {
    }
  }

  protected abstract void work() throws InterruptedException;
}