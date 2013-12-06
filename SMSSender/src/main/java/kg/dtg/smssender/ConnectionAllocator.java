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
public final class ConnectionAllocator  {
  public static String connectionUrl;
  public static Properties properties;

  private ConnectionAllocator() {
  }

  public static void initialize(Properties properties) {
    ConnectionAllocator.properties = new Properties();

    ConnectionAllocator.connectionUrl = properties.getProperty("database.connection.url");

    for (final String propertyName : properties.stringPropertyNames()) {
      if (propertyName.startsWith("database.connection")) {
        final String prop = propertyName.substring(20);
        ConnectionAllocator.properties.setProperty(prop, properties.getProperty(propertyName));
      }
    }
  }

  public static Connection getConnection() throws SQLException {
    final Connection connection = DriverManager.getConnection(connectionUrl, properties);
    connection.setAutoCommit(false);
    return connection;
  }
}