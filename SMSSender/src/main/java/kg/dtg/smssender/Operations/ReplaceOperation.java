package kg.dtg.smssender.Operations;

import kg.dtg.smssender.ShortMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: oleg
 * Date: 3/31/13
 * Time: 3:13 PM
 */
public final class ReplaceOperation extends Operation {
  private final List<ShortMessage> shortMessages = new ArrayList<ShortMessage>();

  public ReplaceOperation(String uid, String sourceNumber, String destinationNumber, String message) {
    super(uid, sourceNumber, destinationNumber, message);
  }

  public final List<ShortMessage> getShortMessages() {
    return shortMessages;
  }

  public final void addShortMessage(ShortMessage shortMessage) {
    shortMessages.add(shortMessage);
  }
}