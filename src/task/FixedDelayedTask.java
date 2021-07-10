package task;


public abstract class FixedDelayedTask extends DelayedTask {

    public FixedDelayedTask(String ID, int delayMS){
        super(ID);

        this.sleepMillis = delayMS;

    }

}
