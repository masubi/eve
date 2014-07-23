package eve.handler;

/*
 * Listener Thread
 * derived from http://docs.oracle.com/javase/tutorial/essential/io/notification.html#overview
 * 
 * 1.  Listens for file system events 
 * 2.  places them in the GlobalTaskQueue
 * 
 * Acts as Producer for TaskQueue
 */


import eve.Main;
import eve.logger.Logger;
import eve.task.Task;
import eve.task.TaskAction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class HandlerThread implements Runnable {
	@Override
	public void run() {
		Logger.info("Handler Thread starting");
        while (Main.shutdownFlag == false) {
            Task nextTask = this.peek();
            if(nextTask != null && nextTask.action != TaskAction.NOACTION){
                Logger.info("Handling "+nextTask.toString());
                executeHandler();
            }
        }
        Logger.info("Handler Thread Shutdown");
	}

	private void executeHandler(){
        try {
            String shell =  Main.config.getProperty("shell");
            String command = Main.config.getProperty("command");

            Process p = Runtime.getRuntime().exec(new String[]{shell,"-c",command});

            //Read output
            StringBuilder out = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null, previous = null;
            while ((line = br.readLine()) != null){
                if (!line.equals(previous)) {
                    previous = line;
                    out.append(line).append('\n');
                    System.out.println(line);
                }
            }

            //Check result
            if (p.waitFor() == 0)
                Logger.debug("Handler success");

            this.pop();//remove head

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private Task peek(){
        return Main.taskQueue.peek();
    }
	
	private Task pop() {
        return Main.taskQueue.popTask();
	}
}