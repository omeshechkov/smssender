package kg.dtg.smssender;

import kg.dtg.smssender.statistic.StatisticCollector;
import kg.dtg.smssender.statistic.StatisticProvider;
import org.apache.log4j.PropertyConfigurator;

import java.io.FileOutputStream;
import java.io.FileReader;
import java.lang.management.ManagementFactory;
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

    FileOutputStream fileOutputStream = new FileOutputStream(properties.getProperty("pid.filename"), false);
    fileOutputStream.write(getPid().getBytes());
    fileOutputStream.close();

    ConnectionAllocator.initialize(properties);

    StatisticProvider.initialize(properties);
    StatisticCollector.initialize(properties);

    HeartbeatDaemon.initialize(properties);

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

  private static String getPid() {
    String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
    int p = nameOfRunningVM.indexOf('@');
    return nameOfRunningVM.substring(0, p);
  }
}