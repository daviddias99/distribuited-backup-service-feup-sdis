package channel;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import client.Peer;
import utils.Protocol;

public abstract class Channel {

    private int port;
    private InetAddress address;
    private MulticastSocket socket;
    private ScheduledThreadPoolExecutor thread_pool;

    protected String debugging_id;

    public Channel(int port, InetAddress address) throws IOException {
        this.port = port;
        this.address = address;
        this.socket = new MulticastSocket(this.port);
        this.socket.joinGroup(this.address);
        this.thread_pool = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(Protocol.MC_THREAD_POOL_SIZE);
        this.debugging_id = "DEFAULT_DBG_ID";
    }

    public int getPort() {
        return this.port;
    }

    public InetAddress getAddress() {
        return this.address;
    }

    @Override
    public String toString() {
        return this.getPort() + ":" + this.getAddress().getHostAddress();
    }

    public void receiveMessage() {

        // Receive messages on the multicast channel
        Runnable task = () -> {

            while (true) {
                
                byte[] buf = new byte[Protocol.PACKET_MAX_SIZE];
                try {
                    
                    DatagramPacket rcvPacket = new DatagramPacket(buf, Protocol.PACKET_MAX_SIZE);
                    System.out.println("Peer " + Peer.getID() + " - " + this.debugging_id + ": " + "Waiting for packets...");
                    this.socket.receive(rcvPacket);
                    System.out.println("Peer " + Peer.getID() + " - " + this.debugging_id + ": " + "Packet Received...");
                    this.handler(rcvPacket);
                } catch (IOException e) {
                    System.out.println("Peer " + Peer.getID() + " - " + this.debugging_id + ": " + "Receive packet error with channel: " + this.address);
                    e.printStackTrace();
                }

            }
        };

        new Thread(task).start();
    }

    public abstract void manage(DatagramPacket packet);

    private void handler(DatagramPacket packet) {

        // Each received message is handled in a separate thread
        Runnable task = () -> {
            this.manage(packet);
        };

        this.thread_pool.execute(task);

    }

    public void sendMessage(byte[] msg) {
        try {
            System.out.println("Peer " + Peer.getID() + " - " + this.debugging_id + ": " + "Sending message...");
            DatagramPacket sndPacket = new DatagramPacket(msg, msg.length, this.address, this.port);
            this.socket.send(sndPacket);
            System.out.println("Peer " + Peer.getID() + " - " + this.debugging_id + ": " + "Message sent...");
        } catch (IOException e) {
            System.err.println(this.debugging_id + ": Error sending message with channel: " + this.address);
            e.printStackTrace();
        }
    }

}