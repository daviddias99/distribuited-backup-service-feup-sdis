package task;

import client.Peer;

public abstract class Task implements Runnable {

    protected String ID;

    public Task(String ID){

        this.ID = ID;

    }

    public String getID(){

        return ID;
    }

    @Override
    public void run() {
       

        if(!Thread.interrupted()){
            this.cancelSelf();
            this.taskFunction();
        }

        this.cancelSelf();
        return;
    }

    public void cancelSelf(){

        Peer.getTaskManager().cancelTask(this.ID);
    }

    protected abstract void taskFunction();

}
