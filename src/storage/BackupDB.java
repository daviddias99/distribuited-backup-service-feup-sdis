package storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import utils.Protocol;

public class BackupDB<A> {
    protected ConcurrentHashMap<String, A> backupDB;

    protected String systemPath;
    private String backupName;

    private int totalSpace;

    public BackupDB(String systemPath, String backupName) {
        this.systemPath = systemPath;
        this.backupName = backupName;

        this.restoreDB();
    }

    /** Restore/Create DBs **/
    private void restoreDB() {
        File f = new File(this.systemPath + "/" + this.backupName);

        // Check if the backed files already exists on disk
        if (f.exists() && !f.isDirectory()) {

            ObjectInputStream backupDBInputStream;
            try {
                backupDBInputStream = new ObjectInputStream(
                        new FileInputStream(this.systemPath + "/" + this.backupName));
                this.backupDB = (ConcurrentHashMap<String, A>) backupDBInputStream.readObject(); // cast is
                                                                                                // needed.
                this.totalSpace = (int) backupDBInputStream.readObject();
                backupDBInputStream.close();
            } catch (final IOException | ClassNotFoundException e) {
                System.out.println("Failed to read Backed Files: " + this.backupName);
                e.printStackTrace();
            }

            System.out.println("Restored Backed Files: " + this.backupName);
        } else {

            this.backupDB = new ConcurrentHashMap<>();
            this.totalSpace = Protocol.DEFAULT_DISK_SIZE;
            System.out.println("Created new Backed Files Ledger: " + this.backupName);
        }
    }

    /** Store DBs **/
    public void storeDB() {
        // Store backed files on disk

        synchronized(this.backupDB){
            try {
                ObjectOutputStream oos = new ObjectOutputStream(
                        new FileOutputStream(this.systemPath + "/" + this.backupName));
    
                oos.writeObject(this.backupDB);
                oos.writeObject(this.totalSpace);
                oos.close();
    
            } catch (IOException e) {
                System.out.println("DB Backup error: " + this.backupName);
                e.printStackTrace();
            }
        }

    }

     /** Has testers **/
     public boolean contains(String key) {
		return this.backupDB.containsKey(key);
    }

    /** Add/Remove from DBs **/
    public void remove(String key) {
        this.backupDB.remove(key);
    }

    public boolean put(String key, A object) {
        
        return this.backupDB.putIfAbsent(key, object) == null ? true : false;
    }

    /** Getter **/
    public A get(String object){
        return this.backupDB.get(object);
    }

    public Set<String> keySet(){
        return this.backupDB.keySet();
    }

    public int getMaxSize(){
        return this.totalSpace;
    }

    public void setMaxSize(int newSize){
        this.totalSpace = newSize;
    }
}