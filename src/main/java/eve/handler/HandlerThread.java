package eve.handler;

import eve.Main;
import eve.logger.Logger;
import eve.task.Task;
import eve.task.TaskAction;
import org.json.simple.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


public class HandlerThread implements Runnable {
	@Override
	public void run() {
		Logger.info("Handler Thread starting");
        while (Main.shutdownFlag == false) {
            Task nextTask =  this.pop();
            if(nextTask != null && nextTask.action != TaskAction.NOACTION){
                Logger.info("Handling "+nextTask.toString());
                executeHandler(nextTask);
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
                Logger.debug("Handler success");

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