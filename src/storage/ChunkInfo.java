package storage;

import java.io.Serializable;
import java.util.ArrayList;

public class ChunkInfo implements Serializable{
    /**
     *
     */
    private static final long serialVersionUID = 6742107957790019640L;
    private int desiredReplication;
    private ArrayList<Integer> currentReplication;

    public ChunkInfo(int desiredReplication){
        this.desiredReplication = desiredReplication;
        this.currentReplication = new ArrayList<>();
    }

    public int getDesiredReplicationDegree() {
        return this.desiredReplication;
    }

    public ArrayList<Integer> getCurrentReplicationList() {
        return this.currentReplication;
    }   

    public int excessRepDegree(){
        return this.getCurrentReplicationDegree() - this.desiredReplication;
    }
    
    public int getCurrentReplicationDegree() {
        return this.currentReplication.size();
    }

    public boolean reachedReplicationDegree(){
        return this.getCurrentReplicationDegree() >= this.desiredReplication;
    }

    public boolean isInPeer(int id){

        return this.currentReplication.contains(id);

    }

    public synchronized void addReplication(int peerID){
        if(!this.currentReplication.contains(peerID))
            this.currentReplication.add(peerID);
    }

	public synchronized void removeReplication(int peerID) {
        for(int i = 0; i < this.getCurrentReplicationDegree(); i++)
            if(this.currentReplication.get(i) ==  peerID){
                this.currentReplication.remove(i);
                return;
            }
    }
    
    @Override
    public String toString(){
        return "Desired: " + desiredReplication + " Replication: " + this.getCurrentReplicationDegree();
    }

	public synchronized boolean hasReplicator(int senderID) {
        for(int i = 0; i < this.getCurrentReplicationDegree(); i++)
            if(this.currentReplication.get(i) ==  senderID)
                return true;
		return false;
	}
}