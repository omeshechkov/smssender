package kg.dtg.smssender;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 4/8/11
 * Time: 2:38 PM
 */
public final class ConnectionAllocator {
  public static String connectionUrl;
  public static Properties properties;

  public static String clusterConnectionUrl;
  public static Properties clusterProperties;

  private ConnectionAllocator() {
  }

  public static void initialize(Properties properties) {
    ConnectionAllocator.properties = new Properties();
    ConnectionAllocator.clusterProperties = new Properties();

    ConnectionAllocator.connectionUrl = properties.getProperty("database.connection.url");
    ConnectionAllocator.clusterConnectionUrl = properties.getProperty("cluster.database.connection.url");

    for (final String propertyName : properties.stringPropertyNames()) {
      if (propertyName.equals("database.connection.url") || propertyName.equals("cluster.database.connection.url"))
        continue;

      if (propertyName.startsWith("database.connection")) {
        final String prop = propertyName.substring("database.connection".length() + 1);
        ConnectionAllocator.properties.setProperty(prop, properties.getProperty(propertyName));
      } else if (propertyName.startsWith("cluster.database.connection")) {
        final String prop = propertyName.substring("cluster.database.connection".length() + 1);
        ConnectionAllocator.clusterProperties.setProperty(prop, properties.getProperty(propertyName));
      }
    }
  }

  public static Connection getConnection() throws SQLException {
    final Connection connection = DriverManager.getConnection(connectionUrl, properties);
    connection.setAutoCommit(false);
    return connection;
  }

  public static Connection getClusterConnection() throws SQLException {
    final Connection connection = DriverManager.getConnection(clusterConnectionUrl, clusterProperties);
    connection.setAutoCommit(false);
    return connection;
  }
}