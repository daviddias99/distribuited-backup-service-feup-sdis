package task;


import java.util.Random;

public abstract class RandomDelayedTask extends DelayedTask {


    public RandomDelayedTask(String ID, int maxDelayMS){
        super(ID);

        Random r = new Random();
        this.sleepMillis = r.nextInt(maxDelayMS);     

    }

}
