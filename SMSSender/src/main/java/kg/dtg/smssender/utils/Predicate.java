package kg.dtg.smssender.utils;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 4/26/11
 * Time: 8:20 PM
 */
public interface Predicate<T> {
  boolean check(T value);
}
