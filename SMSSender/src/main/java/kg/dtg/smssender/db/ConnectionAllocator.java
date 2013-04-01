package kg.dtg.smssender.db;

import kg.dtg.smssender.statistic.MinMaxCounterToken;
import kg.dtg.smssender.statistic.SoftTime;
import org.apache.log4j.Logger;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 4/8/11
 * Time: 2:38 PM
 */
public final class ConnectionAllocator implements Runnable {
  private static final String THREAD_NAME = "Connection allocator";

  public static final Logger LOGGER = Logger.getLogger(ConnectionAllocator.class);

  static ConnectionAllocator instance;

  private final int checkStateInterval;
  private final int checkConnectionTimeout;

  public final String connectionUrl;
  public final Properties properties;

  private final Set<ConnectionToken> registeredConnections = new HashSet<ConnectionToken>();

  private final MinMaxCounterToken checkSingleConnectionTimeCounter;
  private final MinMaxCounterToken checkAllConnectionsTimeCounter;
  private final MinMaxCounterToken allocateConnectionTimeCounter;

  public static void initialize(Properties properties) {
    instance = new ConnectionAllocator(properties);
    new Thread(instance).start();
  }

  private ConnectionAllocator(Properties properties) {
    checkSingleConnectionTimeCounter = new MinMaxCounterToken("Connection allocator: Check single connection time", "milliseconds");
    checkAllConnectionsTimeCounter = new MinMaxCounterToken("Connection allocator: Check all connections time", "milliseconds");
    allocateConnectionTimeCounter = new MinMaxCounterToken("Connection allocator: Allocate connection time", "milliseconds");

    this.properties = new Properties();

    this.connectionUrl = properties.getProperty("database.connection.url");

    for (final String propertyName : properties.stringPropertyNames()) {
      if (propertyName.startsWith("database.connection")) {
        final String prop = propertyName.substring(20);
        this.properties.setProperty(prop, properties.getProperty(propertyName));
      }
    }

    this.checkStateInterval = Integer.parseInt(properties.getProperty("database.checkStateInterval"));
    this.checkConnectionTimeout = Integer.parseInt(properties.getProperty("database.checkConnectionTimeout"));
  }

  static ConnectionToken allocateConnection(final ConnectionToken connectionToken) {
    synchronized (instance.registeredConnections) {
      instance.registeredConnections.add(connectionToken);
      return connectionToken;
    }
  }

  public final void run() {
    LOGGER.info("Connection allocator started");

    try {
      //noinspection InfiniteLoopStatement
      for (; ;) {
        final ConnectionToken[] connectionTokens;
        synchronized (registeredConnections) {
          connectionTokens = registeredConnections.toArray(new ConnectionToken[registeredConnections.size()]);
        }
        final long startCheckAllTime = SoftTime.getTimestamp();

        for (final ConnectionToken connectionToken : connectionTokens) {
          //noinspection SynchronizationOnLocalVariableOrMethodParameter
          synchronized (connectionToken) {
            if (connectionToken.connectionState == ConnectionState.NOT_CONNECTED) {
              connectionToken.close();

              final long allocateConnectionStartTime = SoftTime.getTimestamp();

              try {
                connectionToken.connection = DriverManager.getConnection(connectionUrl, properties);
                connectionToken.connectionState = ConnectionState.CONNECTED;
                connectionToken.dispatcher.setConnectionToken(connectionToken);
              } catch (SQLException e) {
                LOGGER.warn("Cannot allocate connection", e);
              }

              allocateConnectionTimeCounter.setValue(SoftTime.getTimestamp() - allocateConnectionStartTime);
            } else {
              final long checkConnectionStartTime = SoftTime.getTimestamp();

              boolean valid = false;
              try {
                valid = connectionToken.connection.isValid(checkConnectionTimeout);
              } catch (SQLException e) {
                LOGGER.warn("Cannot validate connection", e);
              }

              checkSingleConnectionTimeCounter.setValue(SoftTime.getTimestamp() - checkConnectionStartTime);

              if (!valid) {
                LOGGER.warn("Connection is not valid, reconnect scheduled");
                connectionToken.connectionState = ConnectionState.NOT_CONNECTED;
              }
            }
          }
        }

        checkAllConnectionsTimeCounter.setValue(SoftTime.getTimestamp() - startCheckAllTime);
        Thread.sleep(checkStateInterval);
      }
    } catch (InterruptedException ignored) {
    }
  }

  @Override
  public final String toString() {
    return THREAD_NAME;
  }
}