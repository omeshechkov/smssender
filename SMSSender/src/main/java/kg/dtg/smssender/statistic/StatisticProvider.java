package kg.dtg.smssender.statistic;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 5/25/11
 * Time: 10:21 PM
 */
public final class StatisticProvider implements Runnable {
  private static Logger LOGGER = Logger.getLogger(StatisticProvider.class);

  private final String host;
  private final int port;

  private ServerSocket serverSocket;
  private boolean alive = false;

  public static void initialize(Properties properties) throws IOException {
    new StatisticProvider(properties);
  }

  private StatisticProvider(Properties properties) throws IOException {
    this.host = properties.getProperty("statistic.host");
    this.port = Integer.parseInt(properties.getProperty("statistic.port"));

    startAccept();
    final Thread thread = new Thread(this);
    thread.setName("Statistic provider");
    thread.start();
  }

  private void startAccept() throws IOException {
    serverSocket = new ServerSocket();
    serverSocket.bind(new InetSocketAddress(host, port));
    alive = true;
  }

  @Override
  public final void run() {
    try {
      //noinspection InfiniteLoopStatement
      for (; ; ) {
        if (!alive) {
          try {
            startAccept();
          } catch (IOException e) {
            alive = false;
            LOGGER.warn("Cannot create server socket", e);
            Thread.sleep(5000);
          }
        }

        try {
          final Socket client = serverSocket.accept();
          new StatisticClient(client);
        } catch (IOException e) {
          alive = false;
          LOGGER.warn("Cannot accept client", e);
        }

        Thread.sleep(1);
      }
    } catch (InterruptedException ignored) {
    }
  }
}