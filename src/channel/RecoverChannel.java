package channel;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.directory.InvalidAttributeValueException;
import message.Message;
import message.MessageChunkNo;
import message.MessageParser;
import subprotocol.RestoreSubProtocol;
import utils.Protocol;
import client.Peer;

public class RecoverChannel extends Channel {

    public ConcurrentHashMap<String, MessageChunkNo> chunkStorage;
    public ConcurrentHashMap<String, RestoreSubProtocol> waitingThreads;

    public RecoverChannel(int port, InetAddress address) throws IOException {
        super(port, address);
        this.debugging_id = "RECOVER";
        this.chunkStorage = new ConcurrentHashMap<>();
        this.waitingThreads = new ConcurrentHashMap<>();
    }

    @Override
    public void manage(DatagramPacket packet) {
        byte[] data = packet.getData();

        try {
            Message message = (Message) MessageParser.parse(data, packet.getLength());

            // Ignore wn message
            if (message.getSenderID().equals(Peer.getID() + ""))
                return;

            switch (message.getMessageType()) {
                case CHUNK:

                    // Parse PUTCHUNK message
                    MessageChunkNo msDeg = (MessageChunkNo) message;

                    // Handle the message
                    this.handleChunk(msDeg, packet.getAddress());

                    break;
                default:
                    break;
            }

        } catch (InvalidAttributeValueException e) {
            System.out.println("Failed to parse message in Recover Channel from " + packet.getAddress());
        }
    }

    public void handleChunk(MessageChunkNo msgChnk, InetAddress dpAddress) {

        System.out.println("Peer " + Peer.getID() + " - " + this.debugging_id + ": " + msgChnk.headerToString());

        // Cancel all tasks waiting to send this CHUNK message
        Peer.getTaskManager().cancelTask("MESSAGE_" + msgChnk.getMessageType(), msgChnk.getFileID(),
                msgChnk.getChunkNo());

        if (this.waitingThreads.containsKey(msgChnk.getFileID() + ":" + msgChnk.getChunkNo())) {
            if (msgChnk.getVersion().equals(Protocol.VERSION) && Float.parseFloat(Protocol.VERSION) > 1.01) {
                try {
                    // Initialize socket
                    Socket socket = new Socket(dpAddress, Integer.parseInt(new String(msgChnk.getBody())));

                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                    byte[] chunk = (byte[]) in.readObject();

                    in.close();
                    socket.close();

                    msgChnk.setBody(chunk);

                } catch (NumberFormatException | IOException e) {
                    System.out.println("Could not initiate socket: " + e.toString());
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    System.out.println("Could not read chunk message");
                    e.printStackTrace();
                }

            }

            this.chunkStorage.put(msgChnk.getFileID() + ":" + msgChnk.getChunkNo(), msgChnk);

            RestoreSubProtocol thread = this.waitingThreads.get(msgChnk.getFileID() + ":" + msgChnk.getChunkNo());
            synchronized (thread) {
                thread.notify();
            }

            this.waitingThreads.remove(msgChnk.getFileID() + ":" + msgChnk.getChunkNo());
        }

    }
}