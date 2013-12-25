package kg.dtg.smssender;

import org.apache.log4j.Logger;

import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: Oleg
 * Date: 1/8/13
 * Time: 20:20
 */
public class HeartbeatDaemon extends Dispatcher {
  private static final String THREAD_NAME = "Heartbeat daemon";

  private static final Logger LOGGER = Logger.getLogger(HeartbeatDaemon.class);

  private static long interval;
  private static String nodeName;

  public static void initialize(Properties properties) throws UnknownHostException {
    nodeName = properties.getProperty("node.name");
    interval = Long.parseLong(properties.getProperty("heartbeat"));

    new HeartbeatDaemon();
  }

  @Override
  protected void work() throws InterruptedException {
    Connection connection = null;
    try {
      connection = ConnectionAllocator.getConnection();
      
      final PreparedStatement statement = connection.prepareStatement(
              "replace into `daemon-status` (id, daemon, heartbeat) values (?, ?, ?)"
      );
      statement.setString(1, nodeName);
      statement.setString(2, "SMSSenderHeartbeat");
      statement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));

      statement.executeUpdate();

      connection.commit();
    } catch (Exception e) {
      LOGGER.error("Cannot update heartbeat", e);
    } finally {
      if (connection != null) {
        try {
          connection.close();
        } catch (Exception ignored) {
        }
      }
    }

    Thread.sleep(interval);
  }

  @Override
  public String toString() {
    return THREAD_NAME;
  }
}