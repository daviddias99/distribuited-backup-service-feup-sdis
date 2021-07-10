package task;

import java.io.File;

import client.Peer;
import message.Message;
import message.MessageType;
import utils.Protocol;

public class DelayedControlMessageTask extends RandomDelayedTask{

    private Message msg;
    private MessageType type;
    private int chunkNo;

    public DelayedControlMessageTask(MessageType type,String fileID, int chunkNo, int maxDelayMS, Message message) {
        super("MESSAGE_" + type + ":" +  fileID  + ":" +chunkNo, maxDelayMS);
        this.msg = message;
        this.type = type;
        this.chunkNo = chunkNo;
    }

    @Override
    protected void taskFunction() {
       
        if(Float.parseFloat(Protocol.VERSION) > 1.11 && type.equals(MessageType.ENHANCED_STORED)){

            if(Peer.getBackupSystem().getStoredChunkBackup().getChunkCurrentReplication(msg.getFileID(), chunkNo) >
                Peer.getBackupSystem().getStoredChunkBackup().getChunkDesiredReplication(msg.getFileID(),chunkNo)){

                    File toDeleteFile = new File(Peer.getBackupSystem().getSystemPath() + "/" + msg.getFileID() + "/" + chunkNo);
                    Peer.getBackupSystem().removeChunkFromDisk(msg.getFileID(),chunkNo, toDeleteFile);
                    this.cancelSelf();
                    return;
            }
        }
        

        Peer.getControlChannel().sendMessage(msg.getBytes());
        this.cancelSelf();
    }

}