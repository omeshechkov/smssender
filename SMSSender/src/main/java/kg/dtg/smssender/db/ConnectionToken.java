package kg.dtg.smssender.db;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 4/8/11
 * Time: 2:40 PM
 */
public final class ConnectionToken {
  final ConnectionConsumer dispatcher;

  public ConnectionState connectionState = ConnectionState.NOT_CONNECTED;
  public Connection connection;
  public PreparedStatement[] queryStatements;
  public CallableStatement[] callableStatements;

  public ConnectionToken(final ConnectionConsumer dispatcher) {
    this.dispatcher = dispatcher;

    ConnectionAllocator.allocateConnection(this);
  }

  public final void close() {
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException ignored) {
      }
      connection = null;
    }
  }
}