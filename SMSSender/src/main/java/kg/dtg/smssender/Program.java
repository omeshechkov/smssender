package kg.dtg.smssender;

import kg.dtg.smssender.db.ConnectionAllocator;
import kg.dtg.smssender.statistic.StatisticCollector;
import kg.dtg.smssender.statistic.StatisticProvider;
import org.apache.log4j.PropertyConfigurator;

import java.io.FileReader;
import java.util.Properties;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 4/7/11
 * Time: 5:56 PM
 */
public final class Program {
  public static void main(String[] args) throws Exception {
    PropertyConfigurator.configure("log4j.properties");

    final Properties properties = new Properties();
    properties.load(new FileReader("smssender.properties"));

    HeartbeatDaemon.initialize(properties);

    StatisticCollector.initialize(properties);
    StatisticProvider.initialize(properties);

    ConnectionAllocator.initialize(properties);
    EventDispatcher.initialize(properties);
    SMQueueDispatcher.initialize(properties);
    QueryDispatcher.initialize(properties);

    while (true) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        break;
      }
    }
  }
}