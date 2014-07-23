package eve.task;

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
 * TaskQueue is the bounded buffer design patterr. Producers include Listener,
 * GC, Sync, and Restore Threads. Consumers include single instance of
 * MasterThreadManager. Duplicate tasks for same filePathName are deduplicated
 * by a HashMap named waitingRegistry. If a task arrives in queue same as a
 * previously entered entry in waitingRegistry, the waiting registry is updated
 * and the task is not entered into the queue.
 */
public class TaskQueue implements ITaskQueue {

	//
	// semaphores for producer/consumer model
	//
	// capacity of TaskQueue
	private final int CAPACITY = 20000;
	public Semaphore fillCount = new Semaphore(CAPACITY, true);
	public Semaphore emptyCount = new Semaphore(CAPACITY, true);

	//
	// Main data structures
	//
	private HashMap<String, Task> waitingRegistry = new HashMap<String, Task>(); //k=filepathName, v=Task
	public Queue<Task> waitQueue = new LinkedList<Task>();

	//
	// Statistics variables
	//
	public long statTotTasksPushed = 0;
	public long statTotTasksPopped = 0;
	public long statTotDupeTasks = 0;

	public TaskQueue() {
		fillCount.drainPermits(); // needs to be 0 permits per the
	}

    //
    //  check if path is child of any other paths
    //
    private boolean isNotSubPath(String filePathName){
        Path proposedPath = Paths.get(filePathName);
        for(String regPath: waitingRegistry.keySet()){
            Path existingPath = Paths.get(regPath);
            if(proposedPath.startsWith(existingPath)){
                return false;
            }
        }
        return true;
    }

    // check if child of
    private boolean isSubPathOfConfig(String filePathName){
        Path proposedPath = Paths.get(filePathName);
        Path configPath = Paths.get(eve.Main.config.getProperty("targetDir"));
        if(proposedPath.startsWith(configPath)){
            return true;
        }
        return false;
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
            if(isSubPathOfConfig(t.filePathName)){
                t.filePathName=eve.Main.config.getProperty("targetDir");
            }

            if (waitingRegistry.containsKey(t.filePathName)) {
                Task currTask = waitingRegistry.get(t.filePathName);
                TaskAction ta = dedupeFSEvent(t, currTask);
                t.action = ta;
                waitingRegistry.put(t.filePathName, t);
                statTotDupeTasks++;
                Logger.info("waitingRegistry contains "+t.filePathName);
            } else {
                addToQueueFlag = true;
            }
        }

        if (addToQueueFlag == true) {
            try {
                this.emptyCount.acquire();
            } catch (InterruptedException e) {
                Logger.error(e.toString());
            }
            synchronized (waitingRegistry) {
                Logger.info("waitingRegistry does NOT contain "+t.filePathName);
                waitingRegistry.put(t.filePathName, t);
                synchronized (waitQueue) {
                    waitQueue.add(t);
                }

            }
            this.fillCount.release();
        }

		// Log stats
		synchronized (waitQueue) {
			statTotTasksPushed++;
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
		} else {
			Logger.info("TaskDedupe - unhandled order! curr:"
                    + currTask.action + " newTask:" + newTask.action);
			return TaskAction.MODIFY;
		}

		return result;
	}

	// prior to being called, required semaphores must be called first
	public Task popTask() {
		Task taskToReturn = null;

		try {
			this.fillCount.acquire();
		} catch (InterruptedException e) {
			Logger.error(e.toString());
		}

		synchronized (waitQueue) {
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

		// STATS
		synchronized (waitQueue) {
			statTotTasksPopped++;
		}

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

	public synchronized void printQueue() {
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

	}

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
