package kg.dtg.smssender;

import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class ClusterActivityWatcher extends Dispatcher {
  private static final Logger LOGGER = Logger.getLogger(ClusterActivityWatcher.class);

  private static int clusterId;
  private static String nodeName;
  private static int pollInterval;
  private static int switchInterval;

  private long startSwitchingTime = -1;

  public static void initialize(final Properties properties) {
    ClusterActivityWatcher.clusterId = Integer.parseInt(properties.getProperty("cluster.id"));
    ClusterActivityWatcher.nodeName = properties.getProperty("node.name");
    ClusterActivityWatcher.pollInterval = Integer.parseInt(properties.getProperty("cluster.poll_interval"));
    ClusterActivityWatcher.switchInterval = Integer.parseInt(properties.getProperty("cluster.switch_interval"));

    new ClusterActivityWatcher();
  }

  @Override
  protected void work() throws InterruptedException {
    if (SMQueueDispatcher.getState() != SMDispatcherState.CONNECTED) {
      startSwitchingTime = -1;
      QueryDispatcher.setState(false);
      return;
    }

    try (final Connection connection = ConnectionAllocator.getClusterConnection()) {
      try (final PreparedStatement acquireLockQuery = connection.prepareStatement("select acquire_lock(?, ?)")) {
        acquireLockQuery.setInt(1, clusterId);
        acquireLockQuery.setString(2, nodeName);

        final String currentNodeName;
        try (final ResultSet resultSet = acquireLockQuery.executeQuery()) {
          if (resultSet.next())
            currentNodeName = resultSet.getString(1);
          else
            return;
        }

        connection.commit();

        if (currentNodeName.equals(nodeName)) {
          if (startSwitchingTime == -1) {
            LOGGER.info(String.format("Waiting %s ms to become active...", switchInterval));
            startSwitchingTime = System.currentTimeMillis();
          } else if (System.currentTimeMillis() - startSwitchingTime > switchInterval) {
            QueryDispatcher.setState(true);
          }
        } else {
          startSwitchingTime = -1;
          QueryDispatcher.setState(false);
        }
      }
    } catch (final SQLException e) {
      LOGGER.error("Cluster activity watcher", e);
    }

    Thread.sleep(ClusterActivityWatcher.pollInterval);
  }
}
