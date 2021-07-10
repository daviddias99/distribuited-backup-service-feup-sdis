package task;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import javax.naming.directory.InvalidAttributeValueException;

import client.Peer;
import message.Message;
import message.MessageFactory;
import message.MessageType;
import utils.Protocol;

public class DelayedEnhanceRecoverMessageTask extends RandomDelayedTask {

    private byte[] chunk;
    private String fileID;
    private int chunkNo;

    public DelayedEnhanceRecoverMessageTask(MessageType type, String fileID, int chunkNo, int maxDelayMS,
            byte[] chunk) {
        super("MESSAGE_" + type+ ":" +  fileID +  ":" +chunkNo, maxDelayMS);
        this.chunk = chunk;
        this.chunkNo = chunkNo;
        this.fileID = fileID;
    }

    @Override
    protected void taskFunction() {
        try {
            // Open Socket
            ServerSocket serverSocket = new ServerSocket(0);

            serverSocket.setSoTimeout(Protocol.DELAYED_ENHANCED_RECOVER_SOCKET_TIMEOUT);

            // Build message with Local Port as body of the message and sends it
            Message response = MessageFactory.createChunk(this.fileID, this.chunkNo, null, serverSocket.getLocalPort());
            System.out.println("Open Socket Sender: " + serverSocket.getLocalPort());

            Peer.getRecoverChannel().sendMessage(response.getBytes());

            // Wait for client Socket to be open
            try {
                
                Socket clientSocket = serverSocket.accept();
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                System.out.println("Write Chunk");
    
                out.writeObject(chunk);
    
                System.out.println("Close Chunk Server");
                out.close();
                clientSocket.close();
                serverSocket.close();
            } catch (Exception e) {

                System.out.println("Initiator did not open socket for recovery");
                return ;
            }



        } catch (InvalidAttributeValueException e) {
            System.out.println("Message createChunk failed");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Socket failed");
            e.printStackTrace();
        }

        this.cancelSelf();
    }
}