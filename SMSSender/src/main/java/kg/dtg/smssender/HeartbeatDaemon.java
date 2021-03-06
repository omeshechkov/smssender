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

    try (final Connection connection = ConnectionAllocator.getConnection()) {
      final String updateSql = "update `daemon-status` ds set ds.daemon = ?, ds.heartbeat = ? where ds.id = ?";

      try (final PreparedStatement updateStatement = connection.prepareStatement(updateSql)) {
        updateStatement.setString(1, "SMSSenderHeartbeat");
        updateStatement.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        updateStatement.setString(3, nodeName);

        if (updateStatement.executeUpdate() == 0) {
          final String insertSql = "insert into `daemon-status` (id, daemon, heartbeat) values (?, ?, ?)";
          try (final PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
            insertStatement.setString(1, nodeName);
            insertStatement.setString(2, "SMSSenderHeartbeat");
            insertStatement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));

            insertStatement.execute();
          }
        }
      }

      connection.commit();
    } catch (Exception e) {
      LOGGER.error("Cannot update heartbeat", e);
    }

    new HeartbeatDaemon();
  }

  @Override
  protected void work() throws InterruptedException {
    try (final Connection connection = ConnectionAllocator.getConnection()) {

      final String sql = "update `daemon-status` ds set ds.daemon = ?, ds.heartbeat = ? where ds.id = ?";
      try (final PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, "SMSSenderHeartbeat");
        statement.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
        statement.setString(3, nodeName);

        statement.executeUpdate();
      }

      connection.commit();
    } catch (Exception e) {
      LOGGER.error("Cannot update heartbeat", e);
    }

    Thread.sleep(interval);
  }

  @Override
  public String toString() {
    return THREAD_NAME;
  }
}