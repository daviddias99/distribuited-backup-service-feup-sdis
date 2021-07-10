package task;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import utils.Protocol;

public class TaskManager {

    private ConcurrentHashMap<String, Future<?>> tasks;
    private ScheduledThreadPoolExecutor thread_pool;

    public TaskManager() {
        thread_pool = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(Protocol.TASK_MANAGER_THREAD_POLL_SIZE);
        this.tasks = new ConcurrentHashMap<>();
    }

    public synchronized void addTask(Task task) {

        tasks.put(task.getID(), CompletableFuture.completedFuture(1));
        Future<?> futTask = thread_pool.submit(task);
     
        if(futTask.isDone())
            return;
        tasks.put(task.getID(), futTask);
    }

    public void addDelayedTask(DelayedTask task) {

        tasks.put(task.getID(), thread_pool.schedule(task, task.getSleepMillis(), TimeUnit.MILLISECONDS));
    }

    public boolean cancelTask(String type, String fileID, int chunkNo) {

        boolean result = this.cancelTask(type + ":" + fileID + ":" + chunkNo);
        return result;
    }

    public synchronized boolean cancelTask(String ID) {

        Future<?> task = tasks.remove(ID);
        if (task == null) {
            return false;
        } else {
            
            synchronized (task) {
                return task.cancel(false);
            }
        }
    }

    public synchronized boolean protocolInProgress(String ID){

        for (String key : tasks.keySet()) {
            if(key.contains(ID))
                return true;
        }

        return false;
    }

    public synchronized void printTasks(String message) {

        System.out.println("-------------- Tasks --------------");
        System.out.println(message);
        System.out.println("------------------------------------");
        this.tasks.entrySet().forEach(entry -> {
            System.out.println(entry.getKey() + " - " + entry.getValue().isDone());
        });

        System.out.println("------------------------------------");
    }

    public synchronized String getTasksString(String message) {

        String result = "";
        result += "-------------- Tasks --------------\n";
        result +=message + "\n";
        result +="------------------------------------\n";

        for (String key : tasks.keySet()) {
            result += key;
        }

        result += "------------------------------------";

        return result;
    }


}