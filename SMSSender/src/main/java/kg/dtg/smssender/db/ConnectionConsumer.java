package kg.dtg.smssender.db;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 4/8/11
 * Time: 2:38 PM
 */
public abstract class ConnectionConsumer implements Runnable {
  protected ConnectionToken connectionToken;
  protected ConnectionDispatcherState state = ConnectionDispatcherState.WAIT_CONNECTION_TOKEN;

  public ConnectionConsumer() {
    this.connectionToken = new ConnectionToken(this);

    new Thread(this).start();
  }

  public abstract void connectionTokenAllocated(ConnectionToken connectionToken);

  void setConnectionToken(final ConnectionToken connectionToken) {
    connectionTokenAllocated(connectionToken);

    if (connectionToken.connectionState == ConnectionState.CONNECTED) {
      this.connectionToken = connectionToken;
      state = ConnectionDispatcherState.RUNNING;
    }
  }

  public void setState(final ConnectionDispatcherState state) {
    this.state = state;
  }

  public void run() {
    try {
      //noinspection InfiniteLoopStatement
      for (; ;) {
        if (state != ConnectionDispatcherState.RUNNING) {
          Thread.sleep(1);
          continue;
        }

        if(connectionToken == null) {
            continue;
        }

        //noinspection SynchronizeOnNonFinalField
        synchronized (connectionToken) {
          if (connectionToken.connectionState == ConnectionState.NOT_CONNECTED) {
            state = ConnectionDispatcherState.WAIT_CONNECTION_TOKEN;
            continue;
          }

          work();
          Thread.sleep(1);
        }
      }
    } catch (InterruptedException ignored) {
    }
  }

  protected abstract void work() throws InterruptedException;
}