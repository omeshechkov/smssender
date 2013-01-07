package kg.dtg.smssender.utils;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 4/26/11
 * Time: 6:03 PM
 */
public final class Circular<T> {
  private static final int DEFAULT_SIZE = 16;

  private T[] circle;
  private int size = 0;
  private final AtomicInteger index = new AtomicInteger();

  public Circular() {
    circle = (T[]) new Object[DEFAULT_SIZE];
  }

  public Circular(final int size) {
    circle = (T[]) new Object[size];
  }

  public final void add(final T value) {
    if (size == circle.length) {
      final int length = this.size * 2;
      final T[] circle = (T[]) new Object[length];
      System.arraycopy(this.circle, 0, circle, 0, this.circle.length);

      this.circle = circle;
    }

    circle[size++] = value;
  }

  public final T next() {
    if (size == 0) {
      return null;
    }

    for (; ;) {
      final int current = index.get();
      final int next = current == size - 1 ? 0 : current + 1;

      if (index.compareAndSet(current, next)) {
        return circle[next];
      }
    }
  }

  public final T next(final Predicate predicate) {
    if (size == 0) {
      return null;
    }

    int step = this.size;

    do {
      final int current = index.get();
      final int next = current == size - 1 ? 0 : current + 1;

      if (!index.compareAndSet(current, next)) {
        continue;
      }

      step--;

      final T result;
      if (predicate.check(result = circle[next])) {
        return result;
      }
    }
    while (step != 0);

    return null;
  }

  public final int size() {
    return size;
  }

  public final T[] toArray(final Class<? extends T[]> clazz) {
    return Arrays.copyOf(circle, size, clazz);
  }
}