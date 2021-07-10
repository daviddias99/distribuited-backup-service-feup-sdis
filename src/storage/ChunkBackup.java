package storage;

import java.util.ArrayList;

public class ChunkBackup extends BackupDB<ChunkInfo> {
    public ChunkBackup(String systemPath, String backupName) {
        super(systemPath, backupName);
    }

    public ChunkInfo get(String fileID, int chunkNo){
        return super.get(fileID + ":" + chunkNo);
    }

    public void removeChunk(String fileID, int chunkNo) {
        super.remove(fileID + ":" + chunkNo);
    }

    public boolean hasChunk(String fileID, int chunkNo){
        return super.contains(fileID + ":" + chunkNo);
    }

    public boolean isInLowestReplicators(String fileID, int chunkNo, int peerID, int replicationDifference, int newID){

        ChunkInfo info = this.get(fileID, chunkNo);

        Integer currentCount = 0;

        if(newID < peerID){
            currentCount++;
            
            if(currentCount >= replicationDifference)
                return false;
        }

        ArrayList<Integer> replication = info.getCurrentReplicationList();
        synchronized(replication){

            for (Integer integer : replication) {
                
                if(integer < peerID){

                    currentCount++;

                    if(currentCount >= replicationDifference)
                         return false;
                }

            }

        }

        return true;
    }

    public boolean addChunkReplicator(String fileID, int chunkNo, int peerID) {
        if (!hasChunk(fileID, chunkNo))
            return false;

        ChunkInfo info = this.get(fileID, chunkNo);

        synchronized(info){
            info.addReplication(peerID);
        }  
        return true;
    }

    public boolean removeChunkReplicator(String fileID, int chunkNo, int peerID) {
        if (!hasChunk(fileID, chunkNo))
            return false;

        ChunkInfo info = this.get(fileID, chunkNo);

        synchronized(info){
            info.removeReplication(peerID);
        }
        return true;
    }

    public boolean chunkHasReplicator(String fileID, int chunkNo, int peerID) {
        if(this.get(fileID, chunkNo) == null)
            return false;
        return this.get(fileID, chunkNo).hasReplicator(peerID);
    }

    public boolean chunkHasReplicators(String fileID, int chunkNo) {
        if(this.get(fileID, chunkNo) == null)
            return false;
        return this.get(fileID, chunkNo).getCurrentReplicationDegree() != 0;
    }
}