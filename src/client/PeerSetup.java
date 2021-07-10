package client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import utils.Protocol;

public class PeerSetup {

    private static String protocolVersion;
    private static String ap_temp;
    private static int id;
    private static InetAddress mc_controlChannelAddr;
    private static InetAddress mc_dataBackupChannelAddr;
    private static int mc_controlChannelPort;
    private static int mc_dataRecoverChannelPort;
    private static int mc_dataBackupChannelPort;

    private static Peer peer = null;
    public static void main(String[] args) throws RemoteException {

        // Argument parsing
        if (!parseArguments(args)) {
            System.err.println("Parsing error");
            System.exit(-1);
        }

        Protocol.VERSION = protocolVersion;

        // Channel setup
        try {
            peer = new Peer(id, mc_controlChannelPort, mc_controlChannelAddr, mc_dataBackupChannelPort,
                    mc_dataBackupChannelAddr, mc_dataRecoverChannelPort, mc_dataBackupChannelAddr);

        } catch (IOException e) {
            System.err.println("Error initializing the channels");
            e.printStackTrace();
            System.exit(-2);
        }

        // RMI setup
        PeerInterface stub = (PeerInterface) UnicastRemoteObject.exportObject(peer, 0);

        Registry registry;
        try {
            registry = LocateRegistry.getRegistry();
            registry.rebind(ap_temp, stub);

            System.out.println("RMI: Got local registry");
        } catch (RemoteException e) {

            System.out.println("RMI: Created new registry");
            registry = LocateRegistry.createRegistry(1099);
            registry.rebind(ap_temp, stub);

        }

        Runnable storeTask = () -> {
            Peer.getBackupSystem().storeBackupSystem();
        };

        Runtime.getRuntime().addShutdownHook(new Thread(storeTask));
    }

    private static boolean parseArguments(String[] args) {

        if (args.length != 9) {
            System.err.println(
                    "Usage: BackupSystemPeer <protocol version> <peer id> <service access point> <multicast control channel address> <multicast control channel port> <multicast data backup channel address> <multicast data backup channel port>  <multicast data recover channel address> <multicast data recover channel port> ");
            return false;
        }

        protocolVersion = args[0];
        id = Integer.parseInt(args[1]);
        ap_temp = args[2];

        // Control Channel
        try {
            mc_controlChannelAddr = InetAddress.getByName(args[3]);
        } catch (UnknownHostException e) {
            System.err.println("multicast control channel InetAdress failed");
            return false;
        }
        mc_controlChannelPort = Integer.parseInt(args[4]);

        // Data Backup Channel
        try {
            mc_dataBackupChannelAddr = InetAddress.getByName(args[5]);
        } catch (UnknownHostException e) {
            System.err.println("multicast data backup channel InetAdress failed");
            return false;
        }
        mc_dataBackupChannelPort = Integer.parseInt(args[6]);

        // Data Recover Channel
        try {
            InetAddress.getByName(args[7]);
        } catch (UnknownHostException e) {
            System.err.println("multicast data recover channel InetAdress failed");
            return false;
        }
        mc_dataRecoverChannelPort = Integer.parseInt(args[8]);

        return true;
    }

}