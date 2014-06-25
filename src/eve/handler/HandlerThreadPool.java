
package eve.handler;

import eve.logger.Logger;

import java.io.IOException;
import java.util.ArrayList;

public class HandlerThreadPool {

	private static final int NTHREDS = 1;
	private static ArrayList<Thread> handlerThreadPool =  new ArrayList<Thread>();
	

	public static void start() throws IOException{
		Logger.info("Handler ThreadPool Starting");
		
        Runnable runnable=new HandlerThread();
        Thread handler = new Thread(runnable);
        handler.start();
        handlerThreadPool.add(handler);
	}
	
	/**
	 * HandlerThread needs to be interrupted in order for correct shutdown
	 */
	public static void shutdown(){
		for(Thread t: handlerThreadPool){
			t.interrupt();
		}
	}
	
	/**
	 * Counts number of running threads and returns false if count>1
	 * @return
	 */
	public static boolean isTerminated(){
		int count=0;
		for(Thread t: handlerThreadPool){
			if(t.isAlive()){
				count++;
			}
		}
		
		if(count>0){
			return false;
		}else{
			return true;
		}
	}
}
