package kg.dtg.smssender.statistic;

import org.apache.log4j.Logger;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 5/25/11
 * Time: 8:32 PM
 */
public final class StatisticCollector implements Runnable {
  private static final Logger LOGGER = Logger.getLogger(StatisticCollector.class);

  static StatisticCollector instance;

  private MinMaxCounterToken queueSizeCounter;

  private final Set<Period> periods = new HashSet<Period>();
  private final Map<Period, SnapshotPeriod> periodsMapping = new HashMap<Period, SnapshotPeriod>();

  private final Map<Counter, Period> counters = new HashMap<Counter, Period>();
  private final Map<CounterToken, Set<Counter>> counterTokens = new HashMap<CounterToken, Set<Counter>>();
  private final BlockingQueue<Operation> pendingOperations = new LinkedBlockingQueue<Operation>();

  public static void initialize(Properties properties) {
    if (instance != null)
      return;

    instance = new StatisticCollector();

    final String statisticPeriods = properties.getProperty("statistic.periods");

    final StatisticCollector statisticCollector = StatisticCollector.instance;
    statisticCollector.registerPeriod(Period.UNLIMITED);

    for (final String s : statisticPeriods.split(" ")) {
      if (s != null && !s.isEmpty()) {
        try {
          final int period = Integer.parseInt(s);
          statisticCollector.registerPeriod(period);
        } catch (RuntimeException e) {
          LOGGER.warn(String.format("Cannot parse statistic period number '%s'", s), e);
        }
      }
    }
  }

  private StatisticCollector() {
    queueSizeCounter = new MinMaxCounterToken("Statistic collector queue size", "count");
    new Thread(this).start();
  }

  public final synchronized void registerPeriod(final int periodTime) {
    final Period period = new Period(periodTime);
    final SnapshotPeriod snapshotPeriod = new SnapshotPeriod(periodTime);

    periods.add(period);
    periodsMapping.put(period, snapshotPeriod);

    for (final Map.Entry<CounterToken, Set<Counter>> entry : counterTokens.entrySet()) {
      final CounterToken counterToken = entry.getKey();
      final Set<Counter> counterSet = entry.getValue();

      final Counter counter;
      if (counterToken instanceof MinMaxCounterToken) {
        counter = new MinMaxCounter(counterToken.getName(), counterToken.getMeasureUnit());
      } else {
        counter = new IncrementalCounter(counterToken.getName(), counterToken.getMeasureUnit());
      }

      counterSet.add(counter);
      counters.put(counter, period);
    }
  }

  final void registerOperation(final Operation value) throws InterruptedException {
    pendingOperations.put(value);
  }

  final synchronized MinMaxCounterToken registerMinMaxCounter(final MinMaxCounterToken counterToken) {
    final Set<Counter> counterSet = new HashSet<Counter>();
    counterTokens.put(counterToken, counterSet);

    for (final Period period : periods) {
      final MinMaxCounter counter = new MinMaxCounter(counterToken.name, counterToken.measureUnit);
      counters.put(counter, period);
      counterSet.add(counter);
    }

    return counterToken;
  }

  final synchronized IncrementalCounterToken registerIncrementalCounter(final IncrementalCounterToken counterToken) {
    final Set<Counter> counterSet = new HashSet<Counter>();
    counterTokens.put(counterToken, counterSet);

    for (final Period period : periods) {
      final IncrementalCounter counter = new IncrementalCounter(counterToken.name, counterToken.measureUnit);
      counters.put(counter, period);
      counterSet.add(counter);
    }

    return counterToken;
  }

  final synchronized Map<SnapshotPeriod, List<SnapshotCounter>> takeSnapshot() {
    final Map<SnapshotPeriod, List<SnapshotCounter>> snapshot = new HashMap<SnapshotPeriod, List<SnapshotCounter>>();

    for (final SnapshotPeriod snapshotPeriod : periodsMapping.values()) {
      final List<SnapshotCounter> counterSet = new ArrayList<SnapshotCounter>();
      snapshot.put(snapshotPeriod, counterSet);
    }

    for (final Map.Entry<Counter, Period> entry : counters.entrySet()) {
      final Counter counter = entry.getKey();
      final Period period = entry.getValue();
      final SnapshotPeriod snapshotPeriod = periodsMapping.get(period);

      final SnapshotCounter snapshotCounter;
      if (counter instanceof MinMaxCounter) {
        final MinMaxCounter minMaxCounter = (MinMaxCounter) counter;
        snapshotCounter = new SnapshotMinMaxCounter(counter.getName(), counter.getMeasureUnit(),
                minMaxCounter.getMinValue(), minMaxCounter.getMaxValue());
      } else {
        final IncrementalCounter incrementalCounter = (IncrementalCounter) counter;
        snapshotCounter = new SnapshotIncrementalCounter(counter.getName(), counter.getMeasureUnit(), incrementalCounter.getValue());
      }

      snapshot.get(snapshotPeriod).add(snapshotCounter);
    }

    return snapshot;
  }

  @Override
  public final void run() {
    try {
      int c = 0;
      //noinspection InfiniteLoopStatement
      for (; ; ) {
        try {
          c = ++c % 500; //every 500 iterations measure queue size

          if (c == 499) {
            queueSizeCounter.setValue(pendingOperations.size());
          }

          final Operation operation = pendingOperations.take();

          final CounterToken counterToken = operation.getCounterToken();

          synchronized (this) {
            final Set<Period> elapsedPeriods = new HashSet<Period>();
            for (final Period period : periods) {
              if (period.isElapsed()) {
                elapsedPeriods.add(period);
              }
            }
            final Set<Counter> counterSet = counterTokens.get(counterToken);

            for (final Counter counter : counterSet) {
              final Period period = counters.get(counter);

              if (elapsedPeriods.contains(period)) {
                counter.reset();
              }

              if (operation instanceof SetValueOperation) {
                final MinMaxCounter minMaxCounter = (MinMaxCounter) counter;
                minMaxCounter.setValue(((SetValueOperation) operation).getValue());
              } else {
                final IncrementalCounter incrementalCounter = (IncrementalCounter) counter;
                incrementalCounter.increment();
              }
            }
          }
        } catch (RuntimeException e) {
          LOGGER.warn("An error has been occurred", e);
        }
      }
    } catch (InterruptedException ignored) {
    }
  }
}