package eve.handler;

import eve.Main;
import eve.task.Task;

/**
 * Created with IntelliJ IDEA.
 * User: masuij
 * Date: 8/18/14
 * Time: 12:35 PM
 * Project: oaf
 */
public class RequeueThread implements Runnable {
    Task t=null;
    long timeToSleep=0;

    RequeueThread(Task t, long timeToSleep){
        this.t=t;
        this.timeToSleep=timeToSleep;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(this.timeToSleep);
            Main.taskQueue.pushTask(t);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
