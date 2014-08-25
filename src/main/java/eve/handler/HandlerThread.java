package eve.handler;

import eve.Main;
import eve.logger.Logger;
import eve.task.Task;
import eve.task.TaskAction;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;

public class HandlerThread implements Runnable {
    public static long bufferDelay=2000;

	@Override
	public void run() {
		Logger.info("Handler Thread starting");
        while (Main.shutdownFlag == false) {
            // peek and buffer
            Task check = peek();

            if(check != null && check.action != TaskAction.NOACTION){
                try{
                    Logger.debug("sleeping "+bufferDelay+" ms");
                    Thread.sleep(bufferDelay);
                    Logger.debug("slept "+bufferDelay+" ms");
                }catch(Exception e){
                    Logger.error("Failed to sleep ... eyes in the dark ... one moon circling");
                }

                Task nextTask =  this.pop();

                // ignore no actions
                if(nextTask != null && nextTask.action != TaskAction.NOACTION){
                    Logger.info("Handling " + nextTask.toString());
                    executeHandler(nextTask);
                }
            }
        }
        Logger.info("Handler Thread Shutdown");
	}

	private void executeHandler(Task nextTask){
        try {

            JSONObject directive = Main.directiveTable.get(nextTask.filePathName);

            String shell =  (String) directive.get("shell");
            String command = (String) directive.get("command");

            Process p = Runtime.getRuntime().exec(new String[]{shell,"-c",command});

            //Read output
            StringBuilder out = new StringBuilder();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = null, previous = null;
            while ((line = br.readLine()) != null){
                if (!line.equals(previous)) {
                    previous = line;
                    out.append(line).append('\n');
                    Logger.info(line);
                }
            }

            //Check result
            if (p.waitFor() == 0)
                Logger.debug("Handler success for "+nextTask.toString());

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