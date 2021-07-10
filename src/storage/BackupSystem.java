package storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import utils.Protocol;

public class BackupSystem {

    private final int LEDGER_BACKUP_INTERVAL = Protocol.DISK_BACKUP_INTERVAL_SECONDS; //((seconds)
    private final ScheduledThreadPoolExecutor ledgerBackupScheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
    
    private FileBackup backedFiles;
    private FileBackup deleteFiles;

    private StoredChunkBackup storedChunkLedger;
    private DeleteChunkBackup deleteChunkLedger;

    private int ID;

    private int occupiedSpace = 0;
    private int totalSpace;

    public BackupSystem(int id) {
        this.ID = id;
        new File(this.getSystemPath() + "/").mkdirs();

        backedFiles = new FileBackup(this.getSystemPath(), "backed_files");

        if(Float.parseFloat(Protocol.VERSION) > 1.21){
            deleteFiles = new FileBackup(this.getSystemPath(), "delete_files");
            deleteChunkLedger = new DeleteChunkBackup(this.getSystemPath(), "deleteChunkLedger");
        }

        storedChunkLedger = new StoredChunkBackup(this.getSystemPath(), "storedChunkLedger");
        this.totalSpace = this.storedChunkLedger.getMaxSize();


        this.restoreChunkSpace();
        this.setupDiskBackupThread();
    }

    /** Getters **/
    public String getSystemPath() {
        return "Peers/" + this.ID;
    }

    public FileBackup getBackedFilesDB(){
        return this.backedFiles;
    }

    public FileBackup getDeletedFilesDB(){
        return this.deleteFiles;
    }

    public DeleteChunkBackup getDeleteChunkBackup(){
        return this.deleteChunkLedger;
    }

    public StoredChunkBackup getStoredChunkBackup(){
        return this.storedChunkLedger;
    }

    /** Move between DBs **/
    public void moveBacked2DeleteDB(String fileID) {
        final File dir = new File(this.getSystemPath() + "/" + fileID + "/");
        if(!dir.exists())
            return;
        dir.delete();
        this.deleteFiles.addFile(this.backedFiles.get(fileID));
        this.backedFiles.remove(fileID);
    }

    public void moveStored2DeleteChunkDB(String fileID, int nChunks) {
        for (int i = 0; i < nChunks; i++)
            if(this.storedChunkLedger.chunkHasReplicators(fileID, i)){
                this.deleteChunkLedger.put(fileID + ":" + i, this.storedChunkLedger.get(fileID, i));
                this.storedChunkLedger.removeChunk(fileID, i);
            }
	}

    /** Save / Remove Chunk To Disk **/
    public void saveChunkToDisk(String fileID, int chunkNo, byte[] chunk) throws BackupSystemException, IllegalArgumentException{

        final ChunkInfo newChunk = this.storedChunkLedger.get(fileID, chunkNo);

        if(chunk.length > this.getFreeSpace() || this.totalSpace == 0){

            this.storedChunkLedger.removeChunk(fileID, chunkNo);
            throw new IllegalArgumentException("No available space for chunk number " + chunkNo + " of file: " + fileID);
        }

        // Only store the chunk if it already existed in memory
        if (newChunk == null)
            throw new BackupSystemException("Chunk unknown");

        // Store the chunk on disk
        try {
            
            final Path file = Paths.get(this.getSystemPath() + "/" + fileID + "/" + chunkNo);
            Files.write(file, chunk);

            this.increaseOccupiedSpace(chunk.length);
            this.storedChunkLedger.addChunkReplicator(fileID, chunkNo, this.ID);
        } catch (final IOException e) {
            e.printStackTrace();
            System.out.println("Error on save chunk");

            this.storedChunkLedger.removeChunk(fileID, chunkNo);
        }
    }

    public void removeChunkFromDisk(String fileID, int chunkNo, File deleteFile) {
        this.storedChunkLedger.removeChunk(fileID, chunkNo);
        this.decreaseOccupiedSpace((int)deleteFile.length());
        deleteFile.delete();
    }
  
    public ArrayList<Integer> removeFile(final String fileID) {
        ArrayList<Integer> ret =  new ArrayList<>();
        final File dir = new File(this.getSystemPath() + "/" + fileID + "/");
        if(!dir.exists())
            return ret;

        for(final File file: dir.listFiles()){
            ret.add(Integer.parseInt(file.getName()));
            this.removeChunkFromDisk(fileID, Integer.parseInt(file.getName()), file);
        }

        dir.delete();
        this.backedFiles.remove(fileID);
        return ret;
    }
 
    public byte[] readChunkData(String fileID, int chunkNo) throws IOException {
        final Path file = Paths.get(this.getSystemPath() + "/" + fileID + "/" + chunkNo);
        return Files.readAllBytes(file);
    }
    
    /** Space Operations **/
    public void setTotalSpace(int diskSpace){
        this.totalSpace = diskSpace;
        this.storedChunkLedger.setMaxSize(diskSpace);
    }

    public int getFreeSpace(){
        return this.totalSpace - this.occupiedSpace;
    }

    private synchronized void  increaseOccupiedSpace(int length){
        this.occupiedSpace += length;
    }

    private synchronized void  decreaseOccupiedSpace(int length){
        this.occupiedSpace -= length;
    }

    private void restoreChunkSpace(){
        File[] fileList = new File(this.getSystemPath() + "/").listFiles();
        if (fileList.length == 0)
            return;

        for (File file : fileList) {
            if (file.isDirectory() && !file.getName().equals("recovered_files")) {
                File[] chunks = file.listFiles();
                if (chunks.length == 0)
                    continue;
                for (File chunk : chunks)
                    this.increaseOccupiedSpace((int)chunk.length());
                
            }
        }
    }

    public void storeBackupSystem(){
        this.backedFiles.storeDB();
        this.storedChunkLedger.storeDB();
        
        if(Float.parseFloat(Protocol.VERSION) > 1.21){
            this.deleteFiles.storeDB();
            this.deleteChunkLedger.storeDB();
        }
    }

    /* LEDGER STORAGE */
    private void setupDiskBackupThread() {
        Runnable task = () -> {
            this.storeBackupSystem();            
        };
        ledgerBackupScheduler.scheduleWithFixedDelay(task, 0, LEDGER_BACKUP_INTERVAL, TimeUnit.SECONDS);
    }

    @Override
    public String toString() {

        
        String ret = "--------------------- System Info ----------------------\n";

        ret += " Peer: " + this.ID + "\n";
        ret += " Version: " + Protocol.VERSION + "\n";
        ret += " Storage: " + this.occupiedSpace/1000 + "/" + this.totalSpace/1000 + "KBytes\n";

        ret += "--------------------- Backed Files ---------------------\n";

        int count = 1;
        for (String key : this.backedFiles.keySet()) {
            FileInfo fileInfo = this.backedFiles.get(key);
            ret += " File " + count++ + ":\n";
            ret += " \t- pathname: " + fileInfo.getFilepath() + "\n";
            ret += " \t- id: " + key + "\n";
            ret += " \t- desired repDegree: " + fileInfo.getRepDegree() + "\n";
            ret += " \t- Chunks:\n";
            for (int i = 0; i < fileInfo.getNumberChunks(); i++) {
                ChunkInfo chunkInfo = this.storedChunkLedger.get(key, i);
                if(chunkInfo != null)
                    ret += " \t\t • " + i + " - " + chunkInfo.getCurrentReplicationDegree() + "/"
                        + chunkInfo.getDesiredReplicationDegree() + "\n";
            }
        }

        ret += "--------------------- Chunks Stored --------------------\n";

        File[] fileList = new File(this.getSystemPath() + "/").listFiles();
        if (fileList.length == 0)
            return ret;

        count = 1;
        for (File file : fileList) {
            if (file.isDirectory() && !file.getName().equals("recovered_files")) {
                File[] chunks = file.listFiles();
                if (chunks.length == 0)
                    continue;

                for (File chunk : chunks) {
                    ret += " Chunk " + count++ + ":\n";
                    ret += " \t- id: " + chunk.getName() + " - " + file.getName() + "\n";
                    ret += " \t- size: " + (int) chunk.length() / 1000 + " KBytes\n";
                    ret += " \t- degree: "
                            + this.storedChunkLedger.get(file.getName(), Integer.parseInt(chunk.getName())).getCurrentReplicationDegree()
                            + "\n";
                }
            }
        }

        if(!(Float.parseFloat(Protocol.VERSION) > 1.21))
            return ret;


        ret += "---------------- Files Pending Deletion ----------------\n";
        count = 1;
        for (String key : this.deleteFiles.keySet()) {
            FileInfo fileInfo = this.deleteFiles.get(key);
            ret += " File " + count++ + ":\n";
            ret += " \t- pathname: " + fileInfo.getFilepath() + "\n";
            ret += " \t- id: " + key + "\n";
            ret += " \t- Chunks:\n";
            for (int i = 0; i < fileInfo.getNumberChunks(); i++) {
                ChunkInfo chunkInfo = this.deleteChunkLedger.get(key, i);
                if(chunkInfo != null)
                    ret += " \t\t • " + i + " - " + chunkInfo.getCurrentReplicationDegree() + "\n";
            }
        }

        return ret;
    }
}