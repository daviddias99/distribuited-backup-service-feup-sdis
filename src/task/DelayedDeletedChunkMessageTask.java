package task;

import javax.naming.directory.InvalidAttributeValueException;

import client.Peer;
import message.MessageChunkNo;
import message.MessageFactory;

public class DelayedDeletedChunkMessageTask extends RandomDelayedTask {
    private MessageChunkNo msg;

    public DelayedDeletedChunkMessageTask(String fileID, int chunkNo, int maxDelayMS)
            throws InvalidAttributeValueException {
        super("MESSAGE_DELCHUNK" + ":" + fileID + ":" + chunkNo, maxDelayMS);

        this.msg = MessageFactory.createDelChunk(fileID, chunkNo);
    }

    @Override
    protected void taskFunction() {
        Peer.getControlChannel().sendMessage(msg.getBytes());
        this.cancelSelf();
    }
}

