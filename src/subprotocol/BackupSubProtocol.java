package subprotocol;

import javax.naming.directory.InvalidAttributeValueException;
import message.MessageFactory;
import message.MessageRepDeg;
import client.Peer;
import task.DelayedBackupProtocolTask;
import utils.Protocol;

public class BackupSubProtocol extends SubProtocol {

    
    private MessageRepDeg message;
    private int waitingTimeMS;
    private int currentAttempt;
    private String fileID;
    private int chunkNo;

    public BackupSubProtocol(String fileID, int replicationDegree, int chunkNo, byte[] chunkData, int currentAttempt)
            throws InvalidAttributeValueException {

        // Create the backup message
        message = MessageFactory.createPutChunk(fileID, chunkNo, replicationDegree, chunkData);


        this.fileID = fileID;
        this.chunkNo = chunkNo;

        // Add the chunk to the ledger in order to start keeping track of STORED
        // messages

        this.currentAttempt = currentAttempt;
        this.waitingTimeMS = Protocol.PUTCHUNK_INITIAL_WAITING_TIME;

        for (int i = 1; i <= currentAttempt; i++)
            this.waitingTimeMS *= 2;

    }

    public BackupSubProtocol(String fileID, int replicationDegree, int chunkNo, byte[] chunkData)
            throws InvalidAttributeValueException {
        this(fileID, replicationDegree, chunkNo, chunkData, 0);
        Peer.getBackupSystem().getStoredChunkBackup().addChunk(message.getFileID(), message.getChunkNo(),
                message.getReplicationDegree());
    }

    public BackupSubProtocol(MessageRepDeg message, int currentAttempt){
        this.message = message;

        this.fileID = message.getFileID();
        this.chunkNo = message.getChunkNo();

        this.currentAttempt = currentAttempt;
        this.waitingTimeMS = Protocol.PUTCHUNK_INITIAL_WAITING_TIME;

        // Calculate waiting time
        for (int i = 1; i <= currentAttempt; i++)
            this.waitingTimeMS *= 2;
    }

    @Override
    public void run() {

        // Max retry count surpassed
        if (currentAttempt > Protocol.PUTCHUNK_MAX_RETRY_COUNT) {
            Peer.getTaskManager().cancelTask("SUB_PROTOCOL_DELAYED_BACKUP" + ":" + this.getUID());

            System.out.println("Backup protocol of file " + fileID + ":" + chunkNo+", ended unsucessfully (too many retries)");
            return;
        }

        System.out.println("Backup protocol of file " + fileID + ":" + chunkNo+  " - attempt " + currentAttempt);

        int currentReplication = Peer.getBackupSystem().getStoredChunkBackup().getChunkCurrentReplication(message.getFileID(),
                message.getChunkNo());

        // Replication degree achieved
        if (currentReplication >= message.getReplicationDegree()) {

            Peer.getTaskManager().cancelTask("SUB_PROTOCOL_DELAYED_BACKUP"+ ":" +  this.getUID());
            System.out.println("Backup protocol of file " + fileID + ":" + chunkNo+", ended sucessfully");
            return;
        }

        Peer.getTaskManager().cancelTask("SUB_PROTOCOL_DELAYED_BACKUP"+ ":" +  this.getUID());
        Peer.getBackupChannel().sendMessage(message.getBytes());

        // Instead of waiting using Thread.sleep, schedule a task to be ran after the delay
        Peer.getTaskManager().addDelayedTask(new DelayedBackupProtocolTask(fileID, chunkNo, waitingTimeMS,
                    new BackupSubProtocol(message, currentAttempt + 1)));

        return;
    }

    @Override
    public String getUID() {

        return fileID + ":" + chunkNo;
    }

}
