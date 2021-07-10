package client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Map;

import javax.naming.directory.InvalidAttributeValueException;

import storage.BackupSystem;
import storage.FileInfo;
import channel.*;
import utils.Protocol;
import task.TaskManager;
import task.SubProtocolTask;
import message.MessageFactory;
import message.SimpleMessage;
import subprotocol.*;

public class Peer implements PeerInterface {

    private static int id;
    private static BackupChannel backupChannel;
    private static RecoverChannel recoverChannel;
    private static ControlChannel controlChannel;

    private static BackupSystem backupSystem;

    private static TaskManager taskManager;

    public Peer(int peerID, int mc_controlChannelPort, InetAddress mc_controlChannelAddr, int mc_dataBackupChannelPort,
            InetAddress mc_dataBackupChannelAddr, int mc_dataRecoverChannelPort, InetAddress mc_dataRecoverChannelAddr)
            throws IOException {

        id = peerID;

        controlChannel = new ControlChannel(mc_controlChannelPort, mc_controlChannelAddr);
        backupChannel = new BackupChannel(mc_dataBackupChannelPort, mc_dataBackupChannelAddr);
        recoverChannel = new RecoverChannel(mc_dataRecoverChannelPort, mc_dataRecoverChannelAddr);

        taskManager = new TaskManager();
        backupSystem = new BackupSystem(id);

        // Start channel listening
        controlChannel.receiveMessage();
        backupChannel.receiveMessage();
        recoverChannel.receiveMessage();

        if (Float.parseFloat(Protocol.VERSION) > 1.21) {
            try {
                // Greet other peers
                SimpleMessage msg = MessageFactory.createGreeting();
                controlChannel.sendMessage(msg.getBytes());

                // Try to delete pending files
                Map<String, Integer> fileIDs = backupSystem.getDeleteChunkBackup().getAllPendingDelete();
                for (String fileID : fileIDs.keySet()) {
                    EnhanceDeleteSubProtocol protocol = new EnhanceDeleteSubProtocol(fileID, fileIDs.get(fileID));
                    taskManager.addTask(new SubProtocolTask("DELETE", protocol));
                }

            } catch (InvalidAttributeValueException e) {
                System.out.println("Tried to send Greeting message");
                e.printStackTrace();
            }
        }
    }

    public static TaskManager getTaskManager() {
        return taskManager;
    }

    public static int getID() {
        return id;
    }

    public static BackupSystem getBackupSystem() {
        return backupSystem;
    }

    public static ControlChannel getControlChannel() {
        return controlChannel;
    }

    public static BackupChannel getBackupChannel() {
        return backupChannel;
    }

    public static RecoverChannel getRecoverChannel() {
        return recoverChannel;
    }

    @Override
    public String backup(String filepath, int replicationDegree) throws RemoteException {

        String result = "Backup request status for file: " + filepath + " (r=" + replicationDegree + ")\n";
        FileInfo fileInfo = null;
        ArrayList<byte[]> chunks = new ArrayList<>();

        try {
            fileInfo = new FileInfo(filepath, replicationDegree);
        } catch (Exception e) {
            System.out.println("File Info error: " + e.toString());
            e.printStackTrace();
            return result + "\t-Error: File accessing failed\n";
        }

        if (taskManager.protocolInProgress(fileInfo.getID())) {
            System.out.println("A protocol on this file is currently in progress");
            return result + "\t-Error: A protocol on this file is currently in progress.\n";
        }

        if (!backupSystem.getBackedFilesDB().addFile(fileInfo)) {
            System.out.println("Error: File already backed up.");
            return result + "\t-Error: File already backed up.\n";
        }

        if (Float.parseFloat(Protocol.VERSION) > 1.21
                && backupSystem.getDeletedFilesDB().contains(fileInfo.getID())) {
            backupSystem.getDeletedFilesDB().remove(fileInfo.getID());
            for (int i = 0; i < fileInfo.getNumberChunks(); i++)
                backupSystem.getDeleteChunkBackup().removeChunk(fileInfo.getID(), i);
        }


        try {
            chunks = fileInfo.getChunks();
        } catch (IOException e1) {
            return result + "\t-Error: Couldn't divide file into chunks\n";
        }

        for (int i = 0; i < chunks.size(); i++) {
            BackupSubProtocol protocol = null;
            try {
                protocol = new BackupSubProtocol(fileInfo.getID(), fileInfo.getRepDegree(), i, chunks.get(i));
            } catch (InvalidAttributeValueException e) {
                System.out.println("Failed to initialize Backup SubProtocol for chunk " + i
                        + "\nBeginning deletion of the file: " + filepath);
                this.delete(filepath);
                return result + "\t - Error: Backup SubProtocol Failed in chunk " + i + "\n";
            }
            taskManager.addTask(new SubProtocolTask("BACKUP", protocol));
        }

        result += "\t - OK\n";
        return result;
    }

    @Override
    public String restore(String filepath) throws RemoteException {

        String result = "Restore request status for file: " + filepath + "\n";

        FileInfo fileInfo = null;
        try {
            fileInfo = new FileInfo(filepath);
        } catch (InvalidAttributeValueException e) {
            System.out.println("File Info error: " + e.toString());
            e.printStackTrace();
            return result + "\t-Error: File accessing failed\n";
        }

        // Is not the owner
        if (!backupSystem.getBackedFilesDB().contains(fileInfo.getID())) {

            System.out.println("Only the backup initiator can restore it");
            return result + "\t-Error: Only this file's backup initiator can restore it\n";
        }

        if (taskManager.protocolInProgress(fileInfo.getID())) {
            System.out.println("A protocol on this file is currently in progress");
            return result + "\t-Error: A protocol on this file is currently in progress.\n";
        }

        RestoreSubProtocol protocol = null;
        try {
            protocol = new RestoreSubProtocol(filepath);
        } catch (InvalidAttributeValueException | FileNotFoundException e) {
            return result + "\t - Error: Could not instanciate Restore SubProtocol due to " + e.toString() + " exception\n";
        }

        taskManager.addTask(new SubProtocolTask("RESTORE",protocol));

        result += "\t - OK\n";
        return result ;
    }

    @Override
    public String delete(String filepath) throws RemoteException {
        
        String result = "Delete request status for file: " + filepath + "\n";

        FileInfo fileInfo = null;
        try {
            fileInfo = new FileInfo(filepath);
        } catch (InvalidAttributeValueException e) {
            System.out.println("File Info error on delete");
            e.printStackTrace();
            return result + "\t-Error: File accessing failed\n";
        }

        if(taskManager.protocolInProgress(fileInfo.getID())){
            System.out.println("A protocol on this file is currently in progress");
            return result + "\t-Error: A protocol on this file is currently in progress.\n";
        }

        // Is not the owner
        if(!backupSystem.getBackedFilesDB().contains(fileInfo.getID())){

            System.out.println("Only the backup initiator can delete it");
            return result + "\t-Error: Only this file's backup initiator can delete it\n";
        }

        if(!(Float.parseFloat(Protocol.VERSION) > 1.21)){
            // Send messages to others (repeat some times (??))
            DeleteSubProtocol protocol = null;
            try {
                protocol = new DeleteSubProtocol(fileInfo.getID());
            } catch (InvalidAttributeValueException e) {
                return result + "\t-Error: Failed to initialize Delete SubProtocol\n";
            }

            // Delete own file from chunk Ledger
            for (int i = 0; i < fileInfo.getNumberChunks(); i++)
                backupSystem.getStoredChunkBackup().removeChunk(fileInfo.getID(), i);

            
            taskManager.addTask(new SubProtocolTask("DELETE",protocol));
        } else
            try {
                EnhanceDeleteSubProtocol protocol = new EnhanceDeleteSubProtocol(fileInfo.getID(), fileInfo.getNumberChunks());
                taskManager.addTask(new SubProtocolTask("DELETE",protocol));
            } catch (InvalidAttributeValueException e) {
                System.out.println("Enhance Delete Sub Protocol Failed - couldn't create delete message: " + e.getMessage());
                e.printStackTrace();

                return result + "\t-Error: Enhance Delete Sub Protocol Failed - couldn't create delete message\n"; 
            }

        result += "\t - OK\n";
        return result ;
    }

    @Override
    public String reclaim(int diskSpace) throws RemoteException {
        String result = "Reclaim request status, desired space: " + diskSpace + "KB\n";

        if(diskSpace < 0)
            return result += "\t - Error: The desired space must be a positive number\n";

        backupSystem.setTotalSpace(diskSpace * 1000);
        while (backupSystem.getFreeSpace() < 0 || diskSpace == 0) {
            String chunkID = backupSystem.getStoredChunkBackup().getMostBackedChunk();
            if (chunkID == "")
                return result += "\t - OK\n";;
            
            String[] chunkInfo = chunkID.split(":");

            
            ReclaimSubProtocol protocol = null;
            try {
                protocol = new ReclaimSubProtocol(chunkInfo[0], chunkInfo[1]);
            } catch (InvalidAttributeValueException e) {
                return result += "\t - Error: Couldn't remove the chunk: " + chunkInfo[1] +  "from file " + chunkInfo[0] + "\n";
            }


            // Remove file
            File toDeleteFile = new File(backupSystem.getSystemPath() + "/" + chunkInfo[0] + "/" + chunkInfo[1]);
            backupSystem.removeChunkFromDisk(chunkInfo[0], Integer.parseInt(chunkInfo[1]), toDeleteFile);

            taskManager.addTask(new SubProtocolTask("RECLAIM",protocol));
        }

        return result += "\t - OK\n";
    }

    @Override
    public String state() throws RemoteException {
        return backupSystem.toString();
    }

}