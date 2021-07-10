package task;

import javax.naming.directory.InvalidAttributeValueException;

import storage.BackupSystemException;
import client.Peer;
import message.MessageChunkNo;
import message.MessageFactory;
import message.MessageRepDeg;
import message.MessageType;
import utils.Protocol;

public class DelayedEnhanceBackupMessageTask extends RandomDelayedTask {

    private MessageRepDeg message;
    private MessageChunkNo response;
    private String fileID;
    private int chunkNo;

    public DelayedEnhanceBackupMessageTask(String fileID, int chunkNo, int maxDelayMS,
            MessageRepDeg message) {
        super("DELAYED_BACKUP_ENHANCEMENT" + ":" +  fileID  + ":" +chunkNo, maxDelayMS);
        this.message = message;
        try {
            this.response = MessageFactory.createStored(fileID, chunkNo);
        } catch (InvalidAttributeValueException e) {
            System.out.println("Failed to build response");
            e.printStackTrace();
        }

        this.chunkNo = chunkNo;
        this.fileID = fileID;
    }

    @Override
    protected void taskFunction() {
      
        // Check if replication degree has already been achieved, if so there is no need to store the chunk
        if (Peer.getBackupSystem().getStoredChunkBackup().getChunkCurrentReplication(this.fileID, this.chunkNo) >= message
                .getReplicationDegree()) {
            Peer.getBackupSystem().getStoredChunkBackup().removeChunkCarefully(fileID, chunkNo);

            this.cancelSelf();
            return;
        }

        // Save the chunk
        try {
            Peer.getBackupSystem().saveChunkToDisk(this.fileID, this.chunkNo, message.getBody());
            Peer.getTaskManager().addDelayedTask(new DelayedControlMessageTask(MessageType.ENHANCED_STORED, message.getFileID(), message.getChunkNo(), Protocol.STORED_MSG_DELAY, response));
           
        } catch (BackupSystemException e1) {
            System.out.println("Chunk save failed: " + e1.toString());
            e1.printStackTrace();
            System.exit(-1);
        } catch(IllegalArgumentException e2) {
            System.out.println(e2.getMessage());
        }    

        this.cancelSelf();

    }
}