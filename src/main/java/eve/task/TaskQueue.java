package eve.task;

import eve.Main;
import eve.logger.Logger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

/**
 * TaskQueue is the bounded buffer. Producers include Listener,
 * Duplicate tasks for same filePathName are removed
 * by a HashMap named waitingRegistry. If a task arrives in queue same as a
 * previously entered entry in waitingRegistry, the waiting registry is updated
 * and the task is not entered into the queue.
 */
public class TaskQueue implements ITaskQueue {

    // semaphores for producer/consumer model
    private final int CAPACITY = 20000;  // capacity of TaskQueue
    public Semaphore fillCount = new Semaphore(CAPACITY, true);
    public Semaphore emptyCount = new Semaphore(CAPACITY, true);


    // Main data structures
    private HashMap<String, Task> waitingRegistry = new HashMap<String, Task>(); //k=filepathName, v=Task
    public Queue<Task> waitQueue = new LinkedList<Task>();

    public TaskQueue() {
        fillCount.drainPermits();
    }

    
    private String getSubPathOfConfig(String filePathName){
        Path proposedPath = Paths.get(filePathName);
        for(String userTargetPath: Main.directiveTable.keySet()){
            Path configPath = Paths.get(userTargetPath);
            if(proposedPath.startsWith(configPath)){
                return userTargetPath;
            }
        }
        return null;
    }

    public void pushTask(Task t) {
        if(t==null){
            Logger.info("TaskQueue.pushTask had t==null");
            return;
        }

        boolean addToQueueFlag = false;

        //
        // check if t in taskRegistry-> lk, dedupe, unlk and do NOT add to
        // queue
        //
        synchronized (waitingRegistry) {

            // collect all events under the target directory
            String parentPath = getSubPathOfConfig(t.filePathName);
            if(parentPath!=null){
                t.filePathName=parentPath;
            }

            if (waitingRegistry.containsKey(t.filePathName)) {
                Task currTask = waitingRegistry.get(t.filePathName);
                TaskAction ta = dedupeFSEvent(t, currTask);
                t.action = ta;
                waitingRegistry.put(t.filePathName, t);
                Logger.debug("waitingRegistry contains "+t.filePathName+" updating action to "+t.toString());

                // hack
                if(fillCount.availablePermits()==0)
                    addToQueueFlag=true;

            } else {
                addToQueueFlag = true;
            }
        }

        if (addToQueueFlag == true) {

            //
            //  Begining of bounded buffer add
            //
            try {
                Logger.debug("pushTask() - locking emptyCount="+emptyCount.availablePermits());
                Logger.debug("pushTask() - locking fillCount="+fillCount.availablePermits());
                this.emptyCount.acquire();
            } catch (InterruptedException e) {
                Logger.error(e.toString());
            }

            synchronized (waitingRegistry) {
                Logger.debug("waitingRegistry does NOT contain "+t.filePathName+" so adding");
                Logger.info("adding "+t.toString());
                waitingRegistry.put(t.filePathName, t);
                synchronized (waitQueue) {
                    waitQueue.add(t);
                }

            }
            this.fillCount.release();
            Logger.debug("pushTask() - unlocking emptyCount="+emptyCount.availablePermits());
            Logger.debug("pushTask() - unlocking fillCount="+fillCount.availablePermits());
        }
    }


    
    //
    // Returns the action based on the current TaskAction and newly arrived
    // TaskAction
    // Depending on the new task entered at tail of queue and current task
    // M=Modify, C=Create, D=Delete
    // Back Front Result
    // C D M
    // D C No effect,so put NOACTION
    // D M D
    // M C C
    // M M M
    // C M M -> put modify in this case by default
    // C C C
    // x x x -> if new == current then should be current
    private static synchronized TaskAction dedupeFSEvent(Task newTask,
            Task currTask) {
        TaskAction result = TaskAction.NOACTION;
        if (currTask.action == TaskAction.DELETE
                && newTask.action == TaskAction.CREATE) {
            result = TaskAction.MODIFY;
        } else if (currTask.action == TaskAction.CREATE
                && newTask.action == TaskAction.DELETE) {
            result = TaskAction.NOACTION;
        } else if (currTask.action == TaskAction.MODIFY
                && newTask.action == TaskAction.DELETE) {
            result = TaskAction.DELETE;
        } else if (currTask.action == TaskAction.CREATE
                && newTask.action == TaskAction.MODIFY) {
            result = TaskAction.CREATE;
        } else if (currTask.action == TaskAction.MODIFY
                && newTask.action == TaskAction.MODIFY) {
            result = TaskAction.MODIFY;
        }  else if (currTask.action == TaskAction.MODIFY
                && newTask.action == TaskAction.CREATE) {
            result = TaskAction.MODIFY;
        } else if (currTask.action == newTask.action) {
            result = currTask.action;
        } else if (currTask.action == newTask.action) {
            result = currTask.action;
        } else {
            Logger.info("TaskDedupe - unhandled order! curr:"
                    + currTask.action + " newTask:" + newTask.action + " adding MODIFY");
            return TaskAction.MODIFY;
        }

        return result;
    }

    // prior to being called, required semaphores must be called first
    public Task popTask() {
        Task taskToReturn = null;
        Logger.debug("popTask() - locking emptyCount="+emptyCount.availablePermits());
        Logger.debug("popTask() - locking fillCount="+fillCount.availablePermits());
        try {
            this.fillCount.acquire();
            Logger.debug("popTask() - fillCount acquired="+fillCount.availablePermits());
        } catch (InterruptedException e) {
            return null;
        }

        synchronized (waitingRegistry) {

            // catch nulls
            if (this.isEmpty()) {
                taskToReturn = null;
            } else {
                // remove from queue
                taskToReturn = waitQueue.remove();

                // if file system event -> remove from HashTable
                if (taskToReturn.isFSEvent()) {

                    // waitingRegistry has the most recent TaskAction
                    if (waitingRegistry.containsKey(taskToReturn.filePathName)) {
                        taskToReturn = waitingRegistry
                                .remove(taskToReturn.filePathName);
                    }
                }
                // if not fs event then simply return what was removed from
                // queue
            }
        }

        this.emptyCount.release();
        Logger.debug("popTask() - unlocking emptyCount=" + emptyCount.availablePermits());
        Logger.debug("popTask() - unlocking fillCount="+fillCount.availablePermits());
        return taskToReturn;
    }

    public synchronized Task peek() {
        if (waitQueue.size() > 0)
            return waitQueue.peek();
        return new Task("empty", TaskAction.NOACTION);
    }

    public synchronized int size() {
        return waitQueue.size();
    }

    public synchronized boolean isEmpty() {
        return waitQueue.isEmpty();
    }

/*    public synchronized void printQueue() {
        ArrayList<Task> arr = new ArrayList<Task>();
        synchronized(waitQueue){
            Iterator<Task> j = waitQueue.iterator();
            
            while (j.hasNext()) {
                Task curr = j.next();
                arr.add(curr);
            }
        }
        
        synchronized(waitingRegistry){
            for(Task t : arr){
                // check waitingRegistry for most recent TaskAction
                if (waitingRegistry.containsKey(t.filePathName)) {
                    t.action = waitingRegistry.get(t.filePathName).action;
                }
                Logger.info("     " + t.action + ":" + t.filePathName);
            }
        }

    }*/

    @Override
    public synchronized String toString() {
        StringBuilder sb = new StringBuilder();
        ArrayList<Task> arr = new ArrayList<Task>();
        synchronized(waitQueue){
            for(Task t: waitQueue){
                arr.add(t);
            }
        }
        
        synchronized(waitingRegistry){
            for(Task t : arr){
                // check waitingRegistry for most recent TaskAction
                if (t.isFSEvent() && waitingRegistry.containsKey(t.filePathName)) {
                    t.action = waitingRegistry.get(t.filePathName).action;
                }
                sb.append("     " + t.action + ":" + t.filePathName+"\n");
            }
        }
        
        return sb.toString();
    }
}
