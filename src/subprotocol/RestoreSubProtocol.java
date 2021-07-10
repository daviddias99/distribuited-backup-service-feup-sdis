package subprotocol;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.naming.directory.InvalidAttributeValueException;

import storage.FileInfo;
import client.Peer;
import message.MessageChunkNo;
import message.MessageFactory;
import utils.Protocol;

public class RestoreSubProtocol extends SubProtocol {

    private FileInfo fileInfo = null;
    private FileOutputStream output_file_stream = null;

    public RestoreSubProtocol(String filepath) throws InvalidAttributeValueException, FileNotFoundException {

        fileInfo = new FileInfo(filepath);

        // Create the destination folders and open a new stream
        new File("Peers/" + Peer.getID() + "/recovered_files").mkdirs();

        // Open file output stream
        output_file_stream = new FileOutputStream("Peers/" + Peer.getID() + "/recovered_files/" + fileInfo.getFileName());

    }

    @Override
    public synchronized void run() {

        for (int i = 0; i >= 0; i++) {

            RestoreSubProtocol test = Peer.getRecoverChannel().waitingThreads.putIfAbsent(fileInfo.getID() + ":" + i,
                    this);


            // Restore error
            if (test != null) {

                Peer.getTaskManager().cancelTask("SUB_PROTOCOL_RESTORE"+ ":" +  this.getUID());
                System.out.println("Restore protocol of file " + this.getUID() + ", ended unsucessfully");
                return;
            }

            MessageChunkNo chunk = null;
            
            // Try to restore a chunk
            int j = 0;
            for (; j < Protocol.RESTORE_CHUNK_MAX_TRIES; j++) {
                
                // Send the GETCHUNK message
                try {
                    Peer.getControlChannel().sendMessage(MessageFactory.createGetChunk(fileInfo.getID(), i).getBytes());
    
                } catch (InvalidAttributeValueException e1) {
                    System.out.println("Failed to create GETCHUNK message for file: " + fileInfo.getID() + " - " + i);
                    continue;
                }
    
                // Wait for a notification of the chunk arrival for a maximum of RESTORE_CHUNK_MAX_TIMEOUT_MS
                try {
                    System.out.println("Restore sub protocol started waiting....");
                    wait(Protocol.RESTORE_CHUNK_MAX_TIMEOUT_MS);
                    System.out.println("Restore sub protocol stopped waiting....");
                } catch (InterruptedException e) {
                    continue;
                }
    
                // Get the chunk
                chunk = Peer.getRecoverChannel().chunkStorage.get(fileInfo.getID() + ":" + i);

                if(chunk != null){
                    break;
                }
            }

            // Too many retries for receiving the chunk
            if(j == Protocol.RESTORE_CHUNK_MAX_TRIES - 1){
                System.out.println("Restore protocol of file " + this.getUID() + ", ended unsucessfully (too many retries for chunk " + i + ")");
                break;
            }
           
            // Store the chunk
            try {
                System.out.println("Restore sub protocol wrote bytes");
                output_file_stream.write(chunk.getBody());
            } catch (IOException e) {
                System.out.println("Failed to write chunk " + chunk.getChunkNo()  + " of file " + chunk.getFileID());
                break;
            }

            if(chunk.getBody().length < Protocol.MAX_CHUNK_SIZE){
                try {
                    output_file_stream.close();
                } catch (IOException e) {
                    System.out.println("Failed to close output stream of chunk " + chunk.getChunkNo()  + " from file " + chunk.getFileID());
                }

                System.out.println("Restore protocol of file " + this.getUID() + ", ended sucessfully");
                break;
            }

        }
        
        Peer.getTaskManager().cancelTask("SUB_PROTOCOL_RESTORE"+ ":" +  this.getUID());
    }

    @Override
    public String getUID() {
        return fileInfo.getID();
    }

}