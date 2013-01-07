package kg.dtg.smssender.events;

import kg.dtg.smssender.Event;

import java.sql.Timestamp;

/**
 * Created with IntelliJ IDEA.
 * User: Ernest
 * Date: 15.08.12
 * Time: 17:16
 * To change this template use File | Settings | File Templates.
 */
public class ReplaceSMResponseEvent implements Event {
    private Timestamp commandTimestamp = new Timestamp(System.currentTimeMillis());
    private int commandStatus;
    private int messageId;

    public ReplaceSMResponseEvent(int messageId, int commandStatus) {
        this.messageId = messageId;
        this.commandStatus = commandStatus;
    }

    public Timestamp getCommandTimestamp() {
        return commandTimestamp;
    }

    public void setCommandTimestamp(Timestamp commandTimestamp) {
        this.commandTimestamp = commandTimestamp;
    }

    public int getCommandStatus() {
        return commandStatus;
    }

    public void setCommandStatus(int commandStatus) {
        this.commandStatus = commandStatus;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }
}
