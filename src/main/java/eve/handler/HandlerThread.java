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
    public static long bufferDelay=2000; //amount of time to wait between last execute

	@Override
	public void run() {
		Logger.info("Handler Thread starting");
        while (Main.shutdownFlag == false) {
            Task nextTask =  popNextTask();

            // ignore no actions
            if(nextTask == null || nextTask.action == TaskAction.NOACTION){
                Logger.debug("ignoring NOACTION");
                updateHistoryNoAction(nextTask);
                continue;
            }

            // check within buffer delay
            if(outsideBuffer(nextTask)){
                if(isFirst(nextTask)){
                    updateHistoryRequeue(nextTask);
                    requeueTask(nextTask, getTimeSinceLastExecute(nextTask));
                }else{
                    Logger.info("Handling " + nextTask.toString());
                    executeHandler(nextTask);
                    updateHistoryExecute(nextTask);
                }
            }else{
                updateHistoryRequeue(nextTask);
                requeueTask(nextTask, getTimeSinceLastExecute(nextTask));
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
            if (p.waitFor() == 0){
                Logger.debug("Handler success");
                Logger.debug("executeTimes: " + history.get(nextTask.filePathName).executeTimes +
                    ", noactionEvents: " + history.get(nextTask.filePathName).noactionEvents +
                    ", requeueEvents: " + history.get(nextTask.filePathName).requeues);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    private Task popNextTask() {
        return Main.taskQueue.popTask();
    }

    private void requeueTask(Task t, long timeToSleep){
        Runnable runnable=new RequeueThread(t, timeToSleep);
        Thread handler = new Thread(runnable);
        handler.start();
    }

    //
    //  execute history
    //
    // filePathName history k=filePathName,
    public static class Log {
        public long lastExecuteTime;
        public double noactionEvents;
        public double executeTimes=0;
        public long requeues=0;
        public boolean isfirst=false;

        Log(){
            lastExecuteTime=0;
            noactionEvents=0;
            executeTimes=0;
            requeues=0;
            isfirst=true;
        }
    }
    public static Hashtable<String, Log> history = new Hashtable<String,Log>();

    private static void updateHistoryNoAction(Task t){
        if(!history.containsKey(t.filePathName)){
            Log l = new Log();
            l.noactionEvents=1;
            history.put(t.filePathName, l);
        }else{
            Log l = history.get(t.filePathName);
            l.noactionEvents++;
            history.put(t.filePathName, l);
        }
    }

    public static void updateHistoryExecute(Task t){
        if(!history.containsKey(t.filePathName)){
            Log l = new Log();
            l.executeTimes=1;
            l.lastExecuteTime=System.currentTimeMillis();
            history.put(t.filePathName, l);
        }else{
            Log l = history.get(t.filePathName);
            l.lastExecuteTime=System.currentTimeMillis();
            l.executeTimes++;
            l.isfirst=false;
            history.put(t.filePathName, l);
          }
    }
    
    public static void updateHistoryRequeue(Task t){
        if(!history.containsKey(t.filePathName)){
            Log l = new Log();
            l.requeues=1;
            history.put(t.filePathName, l);
        }else{
            Log l = history.get(t.filePathName);
            l.isfirst=false;
            l.requeues++;
            history.put(t.filePathName, l);
        }
    }

    public static long getTimeSinceLastExecute(Task t){
        if(history.containsKey(t.filePathName)){
            return bufferDelay-(System.currentTimeMillis()-history.get(t.filePathName).lastExecuteTime);
        }else{
            return 0;
        }
    }

    private static boolean isFirst(Task t){
        if(!history.containsKey(t.filePathName)){
            Log l = new Log();
            l.lastExecuteTime=System.currentTimeMillis();
            history.put(t.filePathName,l);
            return true;
        }else{
            Log l = history.get(t.filePathName);
            return l.isfirst;
        }
    }


    private static boolean outsideBuffer(Task t){
        if(!history.containsKey(t.filePathName)){
            Log l = new Log();
            l.lastExecuteTime=System.currentTimeMillis();
            history.put(t.filePathName,l);
            return false;
        }


        if(history.containsKey(t.filePathName)){
            Log l = history.get(t.filePathName);
            long currTime = System.currentTimeMillis();
            if(currTime-l.lastExecuteTime<bufferDelay){
                return false;
            }else{
                Logger.debug("timeSinceLastExecute: "+(currTime-l.lastExecuteTime));
                return true;
            }
        }
        return false;
    }
}