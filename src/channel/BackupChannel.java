package channel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import javax.naming.directory.InvalidAttributeValueException;
import message.Message;
import message.MessageChunkNo;
import message.MessageFactory;
import message.MessageParser;
import message.MessageRepDeg;
import message.MessageType;
import utils.Protocol;
import storage.BackupSystemException;
import storage.FileInfo;
import client.Peer;
import task.DelayedControlMessageTask;
import task.DelayedEnhanceBackupMessageTask;

public class BackupChannel extends Channel {

    public BackupChannel(int port, InetAddress address) throws IOException {
        super(port, address);
        this.debugging_id = "BACKUP";
    }

    @Override
    public void manage(DatagramPacket packet) {
        byte[] data = packet.getData();

        try {
            Message message = (Message) MessageParser.parse(data, packet.getLength());

            switch (message.getMessageType()) {
                case PUTCHUNK:

                    // Parse PUTCHUNK message
                    MessageRepDeg msDeg = (MessageRepDeg) message;
                    System.out.println("Peer " + Peer.getID() + "(" +Thread.currentThread().getId()+")" + " - " + this.debugging_id + ": " +"Message: " + msDeg.headerToString());

                    // Handle the message
                    this.handlePutChunk(msDeg);

                    break;
                default:
                    break;
            }

        } catch (InvalidAttributeValueException e) {
            System.out.println("Failed to parse message in Backup Channel from " + packet.getAddress());
        } catch (BackupSystemException e) { 
            System.out.println("Backup system error while parsing parse message in Backup Channel from " + packet.getAddress());
        }
    }

    public void handlePutChunk(MessageRepDeg msDeg) throws BackupSystemException {
        
        System.out.println("Peer " + Peer.getID() + " - " + this.debugging_id + ": " + msDeg.headerToString());

        // The owner of the file does not store the chunk
        if(Peer.getBackupSystem().getBackedFilesDB().contains(msDeg.getFileID()))
            return;
        
        // If it was previously on "toDelete" DB, removes it
        if(Float.parseFloat(Protocol.VERSION) > 1.21 && Peer.getBackupSystem().getDeletedFilesDB().contains(msDeg.getFileID())){
            FileInfo fileInfo = Peer.getBackupSystem().getDeletedFilesDB().get(msDeg.getFileID());
            for(int i = 0; i < fileInfo.getNumberChunks(); i++)
                Peer.getBackupSystem().getDeleteChunkBackup().removeChunk(fileInfo.getID(), i);
            Peer.getBackupSystem().getDeletedFilesDB().remove(fileInfo.getID());
        }

        byte[] received_chunk = msDeg.getBody();

        // If any delayed backup protocol (consequence of reclaim) is waiting to be ran, stop them
        Peer.getTaskManager().cancelTask("DELAYED_PROTOCOL_BACKUP", msDeg.getFileID(), msDeg.getChunkNo());

        // If the peer already contains the chunk, send a STORED message (ALREADY_STORED)
        if(Peer.getBackupSystem().getStoredChunkBackup().hasChunk(msDeg.getFileID(), msDeg.getChunkNo())){
            
            try {
                MessageChunkNo storedMessage;
                storedMessage = MessageFactory.createStored(msDeg.getFileID(), msDeg.getChunkNo());
                Peer.getTaskManager().addDelayedTask(new DelayedControlMessageTask(MessageType.ALREADY_STORED, msDeg.getFileID(), msDeg.getChunkNo(), Protocol.STORED_MSG_DELAY, storedMessage));
            } catch (InvalidAttributeValueException e) {
                System.out.println("Failed to create stored message when it was already stored");
            }

            return;
        }

        // Add chunk to ledger
        Peer.getBackupSystem().getStoredChunkBackup().addChunk(msDeg.getFileID(), msDeg.getChunkNo(), msDeg.getReplicationDegree());
        
        // Enhanced backup, delay chunk saving
        if(Float.parseFloat(Protocol.VERSION) > 1.11){
            Peer.getTaskManager().addDelayedTask(new DelayedEnhanceBackupMessageTask(msDeg.getFileID(), msDeg.getChunkNo(), Protocol.STORED_MSG_DELAY, msDeg));
            return;
        }
        
        try {

            // Store the chunk in memory
            Peer.getBackupSystem().saveChunkToDisk(msDeg.getFileID(), msDeg.getChunkNo(), received_chunk);
            
        } catch (BackupSystemException e1) {

            e1.printStackTrace();
            System.exit(-1);
        } catch(IllegalArgumentException e2) {
            System.out.println(e2.getMessage());
            Peer.getBackupSystem().getStoredChunkBackup().removeChunk(msDeg.getFileID(), msDeg.getChunkNo());
            throw new BackupSystemException("");
        }

        // Send reply (STORED message)
        MessageChunkNo storedMessage;
        try {
            storedMessage = MessageFactory.createStored(msDeg.getFileID(), msDeg.getChunkNo());
            Peer.getTaskManager().addDelayedTask(new DelayedControlMessageTask(MessageType.STORED, msDeg.getFileID(), msDeg.getChunkNo(), Protocol.STORED_MSG_DELAY, storedMessage));
        } catch (InvalidAttributeValueException e1) {
            System.out.println("Failed to create stored message for a newly stored chunk");
            e1.printStackTrace();
        }
    }
}