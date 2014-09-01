package kg.dtg.smssender;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseFacade {
  public static final int SHORT_MESSAGE_SEQUENCE = 1;

  public static long getSequenceNextValue(final Connection connection, final long sequenceId) throws SQLException {
    try (final PreparedStatement statement = connection.prepareStatement("select get_sequence_next_value(?)")) {
      statement.setLong(1, sequenceId);

      return (Integer)getValue(statement);
    }
  }

  public static List<Object[]> query(final Connection connection, final String sql) throws SQLException {
    try (final PreparedStatement statement = connection.prepareStatement(sql)) {
      return query(statement);
    }
  }

  public static List<Object[]> query(final PreparedStatement statement) throws SQLException {
    final List<Object[]> result = new ArrayList<>();

    try (final ResultSet resultSet = statement.executeQuery()) {
      final int columnsCount = resultSet.getMetaData().getColumnCount();
      while (resultSet.next()) {
        Object[] row = new Object[columnsCount];

        for (int i = 0; i < columnsCount; i++) {
          row[i] = resultSet.getObject(i + 1);
        }

        result.add(row);
      }
    }

    return result;
  }

  public static <T> T getValue(final PreparedStatement statement) throws SQLException {
    try (final ResultSet resultSet = statement.executeQuery()) {
      if (resultSet.next()) {
        return (T)resultSet.getObject(1);
      }
    }

    return null;
  }
}