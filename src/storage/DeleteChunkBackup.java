package storage;

import java.util.HashMap;
import java.util.Map;

public class DeleteChunkBackup extends ChunkBackup {

    public DeleteChunkBackup(String systemPath, String backupName) {
        super(systemPath, backupName);
    }

    public Map<String,Integer> getPendingDelete(String senderID) {
        Map<String,Integer> fileIDs = new HashMap<>();

        for(String key : this.backupDB.keySet())
            if(this.get(key).hasReplicator(Integer.parseInt(senderID))){

                String[] div = key.split(":");
                Integer highestChunk = fileIDs.get(div[0]);

                if(highestChunk != null && highestChunk < Integer.parseInt(div[1])){
                    fileIDs.remove(div[0]);
                    fileIDs.put(div[0], Integer.parseInt(div[1]));
                }
                else if(highestChunk == null)
                    fileIDs.put(div[0], Integer.parseInt(div[1]));

            }
        return fileIDs;
    }
    
    public Map<String,Integer> getAllPendingDelete() {
        Map<String,Integer> fileIDs = new HashMap<>();

        for(String key : this.backupDB.keySet()){
            String[] div = key.split(":");
            Integer highestChunk = fileIDs.get(div[0]);
            if(highestChunk != null && highestChunk < Integer.parseInt(div[1])){
                fileIDs.remove(div[0]);
                fileIDs.put(div[0], Integer.parseInt(div[1]));
            }
            else if(highestChunk == null)
                fileIDs.put(div[0], Integer.parseInt(div[1]));

        }
        return fileIDs;
	}

}