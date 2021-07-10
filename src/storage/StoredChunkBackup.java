package storage;

import java.io.File;

public class StoredChunkBackup extends ChunkBackup {

    public StoredChunkBackup(String systemPath, String backupName) {
        super(systemPath, backupName);
    }

    public void addChunk(String fileID, int chunkNo, int desiredReplicationDeg) {
        this.backupDB.putIfAbsent(fileID + ":" + chunkNo, new ChunkInfo(desiredReplicationDeg));
        new File(this.systemPath + "/" + fileID + "/").mkdir();
    }

    public boolean isChunkInDisk(String fileID, int chunkNo){
        return new File(super.systemPath + "/" + fileID + "/" + chunkNo).exists();
    }

    public void removeChunkCarefully(String fileID, int chunkNo){
        
        if(this.isChunkInDisk(fileID, chunkNo))
            return;
        this.removeChunk(fileID, chunkNo);
    }

    public String getMostBackedChunk(){
        String ret =  "";
        int currentAmount = -10;
        int currentSize = 0;
        int prov;
        for (final String key : this.backupDB.keySet()){

            String[] chunkInfo = key.split(":");
            prov = (int)(new File(this.systemPath + "/" + chunkInfo[0] + "/" + chunkInfo[1])).length();
            
            if(this.get(key).excessRepDegree() > currentAmount){
                currentAmount = this.get(key).excessRepDegree();
                currentSize = prov;
                ret = key;
            }
            else if(this.get(key).excessRepDegree() == currentAmount && prov > currentSize){
                currentSize = prov;
                ret = key;
            }
        }
        
        return ret;
    }
    
    public int getChunkCurrentReplication(String fileID, int chunkNo){
        if(!this.hasChunk(fileID, chunkNo))
            return 0;
        return this.get(fileID, chunkNo).getCurrentReplicationDegree();
    }

    public int getChunkDesiredReplication(String fileID, int chunkNo){
        if(!this.hasChunk(fileID, chunkNo))
            return 0;
        return this.get(fileID, chunkNo).getDesiredReplicationDegree();
    }

    public boolean isChunkReplicated(String fileID, int chunkNo){
        if(!this.hasChunk(fileID, chunkNo))
            return false;
        return this.get(fileID, chunkNo).reachedReplicationDegree();
    }
}