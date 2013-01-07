package kg.dtg.smssender.statistic;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 5/25/11
 * Time: 10:31 PM
 */
public final class StatisticClient implements Runnable {
  private static Logger LOGGER = Logger.getLogger(StatisticClient.class);
  private static final String CANNOT_SEND_STATISTIC_TO_CLIENT_MESSAGE = "Cannot send statistic to client";

  private static final Comparator<SnapshotPeriod> SNAPSHOT_PERIOD_COMPARATOR = new Comparator<SnapshotPeriod>() {
    @Override
    public int compare(SnapshotPeriod o1, SnapshotPeriod o2) {
      if (o1.period > o2.period) {
        return -1;
      }

      if (o1.period < o2.period) {
        return 1;
      }

      return 0;
    }
  };

  private final Socket socket;

  public StatisticClient(final Socket socket) {
    this.socket = socket;
    new Thread(this).start();
  }

  @Override
  public final void run() {
    try {
      final OutputStream outputStream = socket.getOutputStream();
      final PrintWriter printWriter = new PrintWriter(outputStream);
      final StatisticCollector statisticCollector = StatisticCollector.instance;
      final Map<SnapshotPeriod, List<SnapshotCounter>> snapshot = statisticCollector.takeSnapshot();

      final StringBuilder sb = new StringBuilder(2048);
      sb.append("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nConnection: close\r\n\r\n");
      sb.append("<html><head><title>SMSSender statistic</title></head><body>");

      final SnapshotPeriod[] snapshotPeriods = snapshot.keySet().toArray(new SnapshotPeriod[snapshot.size()]);
      Arrays.sort(snapshotPeriods, SNAPSHOT_PERIOD_COMPARATOR);

      for (final SnapshotPeriod period : snapshotPeriods) {
        sb.append("<div class='period'>").append(period.toString()).append("</div>");
        sb.append("<table border='1'>    " +
                "    <thead>\n" +
                "    <tr>\n" +
                "      <th>Counter name</th>\n" +
                "      <th>Value</th>\n" +
                "      <th>Min value</th>\n" +
                "      <th>Max value</th>\n" +
                "      <th>Measure unit</th>\n" +
                "    </tr>\n" +
                "    </thead>");

        final List<SnapshotCounter> counterSet = snapshot.get(period);
        Collections.sort(counterSet);

        for (final SnapshotCounter snapshotCounter : counterSet) {
          sb.append("<tr>")
                  .append("<td>").append(snapshotCounter.getName()).append("</td>");

          if (snapshotCounter instanceof SnapshotMinMaxCounter) {
            final SnapshotMinMaxCounter minMaxCounter = (SnapshotMinMaxCounter) snapshotCounter;
            sb.append("<td>N/A</td>")
                    .append("<td>").append(minMaxCounter.getMinValue()).append("</td>")
                    .append("<td>").append(minMaxCounter.getMaxValue()).append("</td>");
          } else if (snapshotCounter instanceof SnapshotIncrementalCounter) {
            final SnapshotIncrementalCounter incrementalCounter = (SnapshotIncrementalCounter) snapshotCounter;
            sb.append("<td>").append(incrementalCounter.getValue()).append("</td>")
                    .append("<td>N/A</td>")
                    .append("<td>N/A</td>");
          }

          sb.append("<td>").append(snapshotCounter.getMeasureUnit()).append("</td>")
                  .append("</tr>");
        }

        sb.append("</table><br />");
      }

      sb.append("</body></html>");
      printWriter.print(sb);
      printWriter.flush();

      try {
        outputStream.close();
      } catch (IOException ignored) {
      }

      try {
        socket.close();
      } catch (IOException ignored) {
      }
    } catch (IOException e) {
      LOGGER.warn(CANNOT_SEND_STATISTIC_TO_CLIENT_MESSAGE, e);
    }
  }
}