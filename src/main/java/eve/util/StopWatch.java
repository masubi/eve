package eve.util;

import eve.logger.Logger;

public class StopWatch {
    
    private long startTime = 0;
    private long stopTime = 0;
    private boolean running = false;
    private boolean useSimulatedTimeFlag = false;
    
    public StopWatch(boolean simulateTimeFlag){
        this.useSimulatedTimeFlag=simulateTimeFlag;
    }
    
    public synchronized void start() {
        //  needs to be idempotent if called multiple times
        if(this.running==true)
            return;
            
        this.startTime = getCurrentTimeMS();
        this.running = true;
    }

    
    public synchronized void stop() {
        //  needs to be idempotent if called multiple times
        if(this.running==false)
            return;
        
        this.stopTime = getCurrentTimeMS();
        this.running = false;
    }

    /**
     * Stops the timer, resets and starts again
     */
    public synchronized void restart(){
        this.stopTime=0;
        this.startTime= getCurrentTimeMS();
        running=true;
    }
    
    /**
     * Stops timer and returns to initial states
     */
    public synchronized void clear(){
        this.stopTime=0;
        this.startTime=0;
        running=false;
    }
    
    public synchronized boolean isRunning(){
        return running;
    }
    
    public synchronized long getElapsedTime() {
        long elapsed;
        if(running){
            elapsed = (getCurrentTimeMS() - startTime);
        }else{
            elapsed = (stopTime - startTime);
        }
        return elapsed;
    }
    
    public synchronized void printElapsedTimeSecs(){
        long elapsed;
        if (running) {
            elapsed = ((getCurrentTimeMS() - startTime)/1000);
        }else {
            elapsed = ((stopTime - startTime)/1000);
        }
        Logger.info(" Elapsed Time: " + elapsed + "s");
    }

    private long getCurrentTimeMS(){
        return System.currentTimeMillis();
    }
}
