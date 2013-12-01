package kg.dtg.smssender;

import kg.dtg.smssender.db.ConnectionConsumer;
import kg.dtg.smssender.db.ConnectionState;
import kg.dtg.smssender.db.ConnectionToken;
import org.apache.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Properties;

/**
 * Created with IntelliJ IDEA.
 * User: Oleg
 * Date: 1/8/13
 * Time: 20:20
 */
public class HeartbeatDaemon extends ConnectionConsumer {
  private static final String THREAD_NAME = "Heartbeat daemon";

  private static final Logger LOGGER = Logger.getLogger(HeartbeatDaemon.class);
  private static final String QUERY = "replace into `daemon-status` (id, daemon, heartbeat) values (?, ?, ?)";

  private final long interval;
  private final String name;

  public static void initialize(Properties properties) throws UnknownHostException {
    InetAddress address = InetAddress.getLocalHost();

    new HeartbeatDaemon("SMSSenderHeartbeat-" + address.getHostName(), properties);
  }

  private HeartbeatDaemon(String name, Properties properties) {
    this.name = name;
    interval = Long.parseLong(properties.getProperty("heartbeat"));
  }

  @Override
  public final void connectionTokenAllocated(final ConnectionToken connectionToken) {
    LOGGER.info("Connection for heartbeat daemon is successfully allocated");
    connectionToken.queryStatements = new PreparedStatement[1];

    final Connection connection = connectionToken.connection;

    try {
      connectionToken.queryStatements[0] = connection.prepareStatement(QUERY);

      LOGGER.info("Connection for heartbeat daemon is successfully prepared");
    } catch (SQLException e) {
      LOGGER.warn("Cannot prepare queries for heartbeat daemon", e);
      connectionToken.connectionState = ConnectionState.NOT_CONNECTED;
    }
  }

  @Override
  protected void work() throws InterruptedException {
    try {
      PreparedStatement statement = connectionToken.queryStatements[0];
      statement.setString(1, name);
      statement.setString(2, "SMSSenderHeartbeat");
      statement.setTimestamp(3, new Timestamp(System.currentTimeMillis()));

      statement.executeUpdate();
    } catch (Exception e) {
      connectionToken.connectionState = ConnectionState.NOT_CONNECTED;
      LOGGER.error("Cannot update heartbeat", e);
    }

    Thread.sleep(interval);
  }

  @Override
  public String toString() {
    return THREAD_NAME;
  }
}
