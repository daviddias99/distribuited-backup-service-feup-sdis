package subprotocol;

import javax.naming.directory.InvalidAttributeValueException;

import client.Peer;
import message.Message;
import message.MessageFactory;
import task.DelayedEnhanceDeleteProtocolTask;
import utils.Protocol;

public class EnhanceDeleteSubProtocol extends SubProtocol {

    private Message message;
    
    private int waitingTimeMS;
    private int currentAttempt;
    private int nChunks;


    public EnhanceDeleteSubProtocol(String fileID, int nChunks, int currentAttempt) throws InvalidAttributeValueException {

        // Create the backup message
        message = MessageFactory.createDelete(fileID);
        this.nChunks = nChunks;

        this.currentAttempt = currentAttempt;
        this.waitingTimeMS = Protocol.ENHANCED_DELETE_INITIAL_WAITING_TIME;

        for (int i = 1; i <= currentAttempt; i++)
            this.waitingTimeMS *= 2;

    }

    public EnhanceDeleteSubProtocol(Message message, int nChunks, int currentAttempt) {

        // Create the backup message
        message = this.message;
        this.nChunks = nChunks;

        this.currentAttempt = currentAttempt;
        this.waitingTimeMS = Protocol.ENHANCED_DELETE_INITIAL_WAITING_TIME;

        for (int i = 1; i <= currentAttempt; i++)
            this.waitingTimeMS *= 2;

    }

    public EnhanceDeleteSubProtocol(String fileID, int nChunks) throws InvalidAttributeValueException {

        this(fileID, nChunks, 0);

        // Remove own the file from the backup system (make sure it does not delete own chunkledger )
        Peer.getBackupSystem().moveBacked2DeleteDB(message.getFileID());
        Peer.getBackupSystem().moveStored2DeleteChunkDB(message.getFileID(), this.nChunks);
    }

    @Override
    public void run() {

        if (currentAttempt > Protocol.DELETE_MAX_TRIES) {
            Peer.getTaskManager().cancelTask("SUB_PROTOCOL_DELETE"+ ":" +  this.getUID());
            Peer.getTaskManager().cancelTask("DELAYED_PROTOCOL_DELETE"+ ":" +  this.getUID());
            System.out.println("Delete protocol of file " + this.getUID() + ", ended unsucessfully (too many retries)");
            return;
        }
        
        if(!this.hasReplicators()){
            Peer.getTaskManager().cancelTask("SUB_PROTOCOL_DELETE"+ ":" +  this.getUID());
            Peer.getTaskManager().cancelTask("DELAYED_PROTOCOL_DELETE"+ ":" +  this.getUID());
            Peer.getBackupSystem().getDeletedFilesDB().remove(message.getFileID());
            System.out.println("Delete protocol of file " + this.getUID() + ", ended sucessfully");
            return;
        }

        Peer.getControlChannel().sendMessage(message.getBytes());

        Peer.getTaskManager().cancelTask("SUB_PROTOCOL_DELETE"+ ":" +  this.getUID());
        Peer.getTaskManager().cancelTask("DELAYED_PROTOCOL_DELETE"+ ":" +  this.getUID());
        Peer.getTaskManager().addDelayedTask(new DelayedEnhanceDeleteProtocolTask(waitingTimeMS,
                    new EnhanceDeleteSubProtocol(message, nChunks, currentAttempt + 1)));

    }

    private boolean hasReplicators(){
        for (int i = 0; i < this.nChunks; i++)
            if(Peer.getBackupSystem().getDeleteChunkBackup().chunkHasReplicators(message.getFileID(), i))
                return true;
        return false;
    }

    @Override
    public String getUID() {
        
        return message.getFileID();
    }

}