package task;

import client.Peer;
import message.Message;

public class DelayedBackupMessageTask extends RandomDelayedTask {

    private Message msg;

    public DelayedBackupMessageTask(String fileID, int chunkNo, int maxDelayMS, Message message) {
        super("MESSAGE_PUTCHUNK" + ":" +  fileID + ":"  + chunkNo, maxDelayMS);
        this.msg = message;
    }

    @Override
    protected void taskFunction() {
       
        Peer.getBackupChannel().sendMessage(msg.getBytes());
        this.cancelSelf();
    }

}