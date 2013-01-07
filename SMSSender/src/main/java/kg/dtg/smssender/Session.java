package kg.dtg.smssender;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Oleg
 * Date: 4/9/11
 * Time: 7:41 PM
 */
public final class Session {
  private final String uid;
  private final List<ShortMessage> shortMessages = new ArrayList<ShortMessage>();

  public Session(final String uid) {
    this.uid = uid;
  }

  public final String getUid() {
    return uid;
  }

  public final List<ShortMessage> getShortMessages() {
    return shortMessages;
  }
}
