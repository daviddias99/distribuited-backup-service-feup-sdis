package channel;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Map;
import javax.naming.directory.InvalidAttributeValueException;
import message.Message;
import message.MessageChunkNo;
import message.MessageFactory;
import message.MessageParser;
import message.MessageType;
import message.SimpleMessage;
import subprotocol.BackupSubProtocol;
import subprotocol.EnhanceDeleteSubProtocol;
import utils.Protocol;
import client.Peer;
import task.DelayedControlMessageTask;
import task.DelayedDeletedChunkMessageTask;
import task.DelayedEnhanceRecoverMessageTask;
import task.DelayedReclaimBackupProtocolTask;
import task.DelayedRecoverMessageTask;
import task.SubProtocolTask;

public class ControlChannel extends Channel {

    public ControlChannel(int port, InetAddress address) throws IOException {
        super(port, address);
        this.debugging_id = "CONTROL";
    }

    @Override
    public void manage(DatagramPacket packet) {
        byte[] data = packet.getData();

        try {
            SimpleMessage message = MessageParser.parse(data,packet.getLength());

            // Ignore own message
            if (message.getSenderID().equals(Peer.getID() + ""))
                return;

            switch (message.getMessageType()) {
                case STORED:
                    this.handleStored((MessageChunkNo) message);
                    break;
                case GETCHUNK:
                    this.handleGetChunk((MessageChunkNo) message);
                    break;
                case DELETE:
                    this.handleDelete((Message) message);
                    break;
                case REMOVED:
                    this.handleRemoved((MessageChunkNo) message);
                    break;
                case GREETING:
                    this.handleGreeting(message);
                    break;
                case DELCHUNK:
                    this.handleDeletedChunk((MessageChunkNo) message);
                    break;
                default:
                    break;
            }

        } catch (InvalidAttributeValueException e) {
            System.out.println("Failed to parse message in Control Channel from " + packet.getAddress());
        }
    }

    private void handleGreeting(SimpleMessage message) {
        
        System.out.println("Peer " + Peer.getID() + " - " + this.debugging_id + ": " + message.headerToString());

        // The greeting message is part of the DELETE enhancement
        if(!(Float.parseFloat(Protocol.VERSION) > 1.21))
            return;
        
        // Check if the peer that sent the greet has any pending deletions
        Map<String,Integer> fileIDs = Peer.getBackupSystem().getDeleteChunkBackup().getPendingDelete(message.getSenderID());
        for(String fileID : fileIDs.keySet())
            try {
                EnhanceDeleteSubProtocol protocol = new EnhanceDeleteSubProtocol(fileID, fileIDs.get(fileID));
                Peer.getTaskManager().addTask(new SubProtocolTask("DELETE",protocol));
            } catch (InvalidAttributeValueException e) {
                System.out.println("Enhance Delete Sub Protocol Failed - couldn't create delete message: " + e.getMessage());
            }

    }

    private void handleDeletedChunk(MessageChunkNo message) {

        System.out.println("Peer " + Peer.getID() + " - " + this.debugging_id + ": " + message.headerToString());

        // The deleted chunk message is part of the DELETE enhancement
        if(!(Float.parseFloat(Protocol.VERSION) > 1.21))
            return;

        // If not the owner
        if(!Peer.getBackupSystem().getDeletedFilesDB().contains(message.getFileID()))
            return;
        
        // Remove peer from deleted chunk replicators
        Peer.getBackupSystem().getDeleteChunkBackup().removeChunkReplicator(message.getFileID(), message.getChunkNo(), Integer.parseInt(message.getSenderID()));
        
        // If no more replicators remove chunk
        if(!Peer.getBackupSystem().getDeleteChunkBackup().chunkHasReplicators(message.getFileID(), message.getChunkNo()))
            Peer.getBackupSystem().getDeleteChunkBackup().removeChunk(message.getFileID(), message.getChunkNo());
    }

    private void handleGetChunk(MessageChunkNo message) {
        System.out.println("Peer " + Peer.getID() + " - " + this.debugging_id + ": " + message.headerToString());
       
        // If a peer does not store the chunk he can't respond to the restore subprotocol
        if(!Peer.getBackupSystem().getStoredChunkBackup().hasChunk(message.getFileID(),message.getChunkNo()))
            return;

        // Obtain the chunk
        byte[] chunk = null;
        try {
            chunk = Peer.getBackupSystem().readChunkData(message.getFileID(), message.getChunkNo());
        } catch (IOException e1) {
            System.out.println("Can't read chunk " + message.getChunkNo() + " from file " + message.getFileID() + "\nAborting Get Chunk handler");
            return;
        }

        // Send the chunk through a TCP channel or through the MDR channel depending on version
        if(message.getVersion().equals(Protocol.VERSION) && Float.parseFloat(message.getVersion()) > 1.01)
            Peer.getTaskManager().addTask(new DelayedEnhanceRecoverMessageTask(MessageType.CHUNK, message.getFileID(), message.getChunkNo(), Protocol.CHUNK_MSG_DELAY, chunk));
        else
            try {
                Message response = MessageFactory.createChunk(message.getFileID(), message.getChunkNo(), chunk, -1);
                Peer.getTaskManager().addTask(new DelayedRecoverMessageTask(MessageType.CHUNK, message.getFileID(), message.getChunkNo(), Protocol.CHUNK_MSG_DELAY, response));
            } catch (InvalidAttributeValueException e) {
                System.out.println("Failed to create Chunk Message");
            }

    }

    private void handleDelete(Message message) {

        System.out.println("Peer " + Peer.getID() + " - " + this.debugging_id + ": " + message.headerToString());

        // Remove all chunks related to file form the system
        ArrayList<Integer> removedChunks = Peer.getBackupSystem().removeFile(message.getFileID());

        if(!(Float.parseFloat(Protocol.VERSION) > 1.21))
            return;

        if(!(Float.parseFloat(message.getVersion()) > 1.21))
            return;
        
            // Peers implementing the enhanced version also respond with a DELCHUNK message
        for(int i = 0; i < removedChunks.size(); i++)
            try {
                Peer.getTaskManager().addTask(new DelayedDeletedChunkMessageTask(message.getFileID(),
                        removedChunks.get(i), Protocol.CHUNK_MSG_DELAY));
            } catch (InvalidAttributeValueException e) {
                System.out.println("Couldn't build message for chunk " + removedChunks.get(i) + " - " + e.getMessage());
            }
    }

    private void handleStored(MessageChunkNo message) {
        
        System.out.println("Peer " + Peer.getID() + " - " + this.debugging_id + ": " + message.headerToString());

        if(Peer.getBackupSystem().getStoredChunkBackup().chunkHasReplicator(message.getFileID(), message.getChunkNo(),Integer.parseInt(message.getSenderID()) ))
            return;

        // Last resort before sending stored (enhanced backup)
        // Current Replication includes newly added chunk, if the needed replication as been achieved then it can be replaced by the one in the other peer
        // If he is the file owner he should not remove any chunk (he doesn't have them)
        if(this.isReplicationEnsured(message) && !this.isFileOwner(message.getFileID()) && this.supportsBackupEnhancement()){


            // Cancel any stored messages for this chunk
            
            boolean enhancedStoredMessagesCanceled = Peer.getTaskManager().cancelTask("MESSAGE_ENHANCED_STORED", message.getFileID(), message.getChunkNo());
            boolean alreadyStoredMessagesCanceled = Peer.getTaskManager().cancelTask("MESSAGE_ALREADY_STORED", message.getFileID(), message.getChunkNo()); 


            // If a enhanced stored message was canceled then we need to remove the chunk from the disk
            if(enhancedStoredMessagesCanceled){

                this.removeFile(message.getFileID(), message.getChunkNo());
  
                return;
            }
            
            boolean isChunkInDisk = Peer.getBackupSystem().getStoredChunkBackup().isChunkInDisk(message.getFileID(), message.getChunkNo());
            int replicationDifference = Peer.getBackupSystem().getStoredChunkBackup().getChunkCurrentReplication(message.getFileID(), message.getChunkNo())  - Peer.getBackupSystem().getStoredChunkBackup().getChunkDesiredReplication(message.getFileID(), message.getChunkNo());
            boolean isInLowestReplicators = Peer.getBackupSystem().getStoredChunkBackup().isInLowestReplicators(message.getFileID(), message.getChunkNo(), Peer.getID(),replicationDifference + 1, Integer.parseInt(message.getSenderID()));

            if(isChunkInDisk && isInLowestReplicators){
                
                this.removeFile(message.getFileID(), message.getChunkNo());   
                this.sendRemovedMessage(message.getFileID(), message.getChunkNo());
                return;
            } 
            else if(alreadyStoredMessagesCanceled){
                try {
                    MessageChunkNo storedMessage;
                    storedMessage = MessageFactory.createStored(message.getFileID(), message.getChunkNo());
                    Peer.getTaskManager().addDelayedTask(new DelayedControlMessageTask(MessageType.ALREADY_STORED, message.getFileID(), message.getChunkNo(), Protocol.STORED_MSG_DELAY, storedMessage));
                } catch (InvalidAttributeValueException e) {
                    System.out.println("Failed to create stored message when it was already stored");
                }
            }
        }
            
        Peer.getBackupSystem().getStoredChunkBackup().addChunkReplicator(message.getFileID(), message.getChunkNo(), Integer.parseInt(message.getSenderID()));
    }

    private void sendRemovedMessage(String fileID, int chunkNo){
        try {
            Message msg = MessageFactory.createRemoved(fileID, chunkNo);
            Peer.getTaskManager().addDelayedTask(new DelayedControlMessageTask(MessageType.REMOVED,fileID,  chunkNo, Protocol.RECLAIM_PUTCHUNK_MSG_DELAY, msg));
        } catch (InvalidAttributeValueException e) {
            System.out.println("Failed to create Removed message in backup protocol. Did not delete chunk " + chunkNo + " from file "  + fileID);
        }
    }

    private void removeFile(String fileID, int chunkNo){

        File toDeleteFile = new File(Peer.getBackupSystem().getSystemPath() + "/" + fileID + "/" + chunkNo);
        Peer.getBackupSystem().removeChunkFromDisk(fileID, chunkNo, toDeleteFile);
    }

    private void handleRemoved(MessageChunkNo message) {
        System.out.println("Peer " + Peer.getID() + " - " + this.debugging_id + ": " + message.headerToString());

        // Remove senders id from the chunks replicators
        if(!Peer.getBackupSystem().getStoredChunkBackup().removeChunkReplicator(message.getFileID(), message.getChunkNo(), Integer.parseInt(message.getSenderID())))
            return;

        // if the chunk is still replication no more operations are needed
        if(Peer.getBackupSystem().getStoredChunkBackup().isChunkReplicated(message.getFileID(), message.getChunkNo()))
            return;

        // Check If this peer has the chunk backed up
        if(Peer.getBackupSystem().getBackedFilesDB().contains(message.getFileID()))
            return;

        int repDeg = Peer.getBackupSystem().getStoredChunkBackup().getChunkDesiredReplication(message.getFileID(), message.getChunkNo());

        // Execute a backup subprtocool for the removed chunk
        try {

            BackupSubProtocol prot = new BackupSubProtocol(message.getFileID(), repDeg,
                    message.getChunkNo(), Peer.getBackupSystem().readChunkData(message.getFileID(), message.getChunkNo()));
                    
            Peer.getTaskManager().addTask(new DelayedReclaimBackupProtocolTask( message.getFileID(),  message.getChunkNo(), Protocol.RECLAIM_PUTCHUNK_MSG_DELAY, prot));
            
        } catch (IOException e) {
            System.out.println("Failed to read chunk " + message.getChunkNo() + " data from file " + message.getFileID() + "\nBackupProtocol was not initialized");
        } catch (InvalidAttributeValueException e) {
            System.out.println("Failed to start backup SubProtocol in response to removed message");
        }
    }

    private boolean supportsBackupEnhancement(){

        return Float.parseFloat(Protocol.VERSION) > 1.11;
    }

    private boolean isReplicationEnsured(MessageChunkNo message){

        return Peer.getBackupSystem().getStoredChunkBackup().getChunkCurrentReplication(message.getFileID(), message.getChunkNo()) >=
        Peer.getBackupSystem().getStoredChunkBackup().getChunkDesiredReplication(message.getFileID(), message.getChunkNo());
    }

    private boolean isFileOwner(String fileID){

        return Peer.getBackupSystem().getBackedFilesDB().contains(fileID);
    }
}