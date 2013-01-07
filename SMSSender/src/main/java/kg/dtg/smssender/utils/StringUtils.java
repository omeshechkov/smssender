package kg.dtg.smssender.utils;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 7/17/11
 * Time: 7:39 PM
 */
public final class StringUtils {
  public StringUtils() throws IllegalAccessException {
    throw new IllegalAccessException();
  }

  public static String[] split(final String message, final int size) {
    final int count = message.length() / (size + 1) + 1;
    final String[] result = new String[count];

    int start = 0;
    int i = 0;
    do {
      int end = start + size;

      if (end >= message.length()) {
        end = message.length();
      }

      result[i++] = message.substring(start, end);
      start = end;
    } while (start != message.length());

    return result;
  }
}
