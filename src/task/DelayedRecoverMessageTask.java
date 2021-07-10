package task;

import client.Peer;
import message.Message;
import message.MessageType;

public class DelayedRecoverMessageTask extends RandomDelayedTask {

    private final Message msg;

    public DelayedRecoverMessageTask(final MessageType type, final String fileID, final int chunkNo, final int maxDelayMS, final Message message) {
        super("MESSAGE_" + type+ ":" +  fileID  + ":" + chunkNo, maxDelayMS);
        this.msg = message;
    }

    @Override
    protected void taskFunction() {
        Peer.getRecoverChannel().sendMessage(msg.getBytes());
        this.cancelSelf();
    }
}