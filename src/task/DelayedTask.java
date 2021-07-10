package task;

public abstract class DelayedTask extends Task {

    protected int sleepMillis;

    public DelayedTask(String ID){
        super(ID);
    }

    public String getID(){

        return ID;
    }

    public int getSleepMillis(){
        return sleepMillis;
    }

    protected abstract void taskFunction();

}
