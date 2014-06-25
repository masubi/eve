package eve.util;


import eve.logger.Logger;

/**
 * Adapted from Source:  http://www.goldb.org/stopwatchjava.html
 * 
 *
 */

/*
Copyright (c) 2005, Corey Goldberg

StopWatch.java is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.
*/

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
    
    //elaspsed time in milliseconds
    public synchronized long getElapsedTime() {
        long elapsed;
        if (running) {
             elapsed = (getCurrentTimeMS() - startTime);
        }
        else {
            elapsed = (stopTime - startTime);
        }
        return elapsed;
    }
    
    
    //elaspsed time in seconds
    public synchronized long getElapsedTimeSecs() {
        long elapsed;
        if (running) {
            elapsed = ((getCurrentTimeMS() - startTime) / 1000);
        }
        else {
            elapsed = ((stopTime - startTime) / 1000);
        }
        return elapsed;
    }

    public synchronized void printElapsedTimeMillisSecs(){
        long elapsed;
        if (running) {
            elapsed = ((getCurrentTimeMS() - startTime));
        }
        else {
            elapsed = ((stopTime - startTime));
        }
        Logger.info("Elapsed Time: " + elapsed + "ms");
    }
      
    public synchronized void printElapsedTimeSecs(){
        long elapsed;
        if (running) {
            elapsed = ((getCurrentTimeMS() - startTime)/1000);
        }
        else {
            elapsed = ((stopTime - startTime)/1000);
        }
        Logger.info(" Elapsed Time: " + elapsed + "s");
    }

    private long getCurrentTimeMS(){
        return System.currentTimeMillis();
    }

    //sample usage
    public static void main(String[] args) {
        StopWatch s = new StopWatch(false);
        s.start();
        //code you want to time goes here
        s.stop();
        Logger.info("elapsed time in milliseconds: " + s.getElapsedTime());
    }
}
