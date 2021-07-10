package client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestApp {
    public static void main(String[] args) {
        if (args.length > 4 || args.length < 2) {
            System.err.println("Usage: TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2> ");
            System.exit(-1);
        }

        String peer_ap = args[0];
        String sub_protocol = args[1];

        try {
            Registry registry = LocateRegistry.getRegistry();
            PeerInterface stub = (PeerInterface) registry.lookup(peer_ap);

            switch (sub_protocol) {
                case "BACKUP":
                    if(args.length != 4){
                        System.err.println("Usage: TestApp <peer_ap> BACKUP <filepath> <replication_degree> ");
                        System.exit(-1);
                    }
                    System.out.println(stub.backup(args[2], Integer.parseInt(args[3])));
					break;
                case "RESTORE":
                    if(args.length != 3){
                        System.err.println("Usage: TestApp <peer_ap> RESTORE <filepath> ");
                        System.exit(-1);
                    }
					System.out.println(stub.restore(args[2]));
                    break;
                case "DELETE":
                    if(args.length != 3){
                        System.err.println("Usage: TestApp <peer_ap> DELETE <filepath> ");
                        System.exit(-1);
                    }
                    System.out.println(stub.delete(args[2]));
                    break;
                case "RECLAIM":
                    if(args.length != 3){
                        System.err.println("Usage: TestApp <peer_ap> RECLAIM <diskSpace> ");
                        System.exit(-1);
                    }
                    System.out.println(stub.reclaim(Integer.parseInt(args[2])));
                    break;
                case "STATE":
                    if(args.length != 2){
                        System.err.println("Usage: TestApp <peer_ap> STATE ");
                        System.exit(-1);
                    }
                    System.out.println(stub.state());
                    break;

                default:
                    System.err.println("Invalid Option\nTry: BACKUP, RESTORE, DELETE, RECLAIM, STATE");
                    System.exit(-1);
					break;
			}
        } catch (Exception e) {
			System.out.println("TestApp exception: " + e.toString());
            e.printStackTrace();
        } 
    }

}