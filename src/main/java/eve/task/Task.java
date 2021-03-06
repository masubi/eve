package eve.task;

import java.io.Serializable;
import java.util.Date;

public class Task implements Serializable{
    private static final long serialVersionUID = -3339475349150893339L;
    public String filePathName;
    public TaskAction action;
    public TaskStatus status;
    public int numProcessAttempts;
    public Date date;
    public boolean deferred=false;
    private long timestamp;
    
    public enum TaskStatus {
        WAITING, COMPLETE
    }
    
    /**
     * Constructor for file system events
     */
    public Task(String newFilePathName, TaskAction newAction) {
        this.action = newAction;
        this.filePathName = newFilePathName;
        this.status = TaskStatus.WAITING;
        this.numProcessAttempts=0;
        this.createTimeStamp();
    }

    public Task(String newFilePathName, TaskAction newAction, TaskStatus s) {
        this.action = newAction;
        this.filePathName = newFilePathName;
        this.status = s;
        this.status = TaskStatus.WAITING;
        this.numProcessAttempts=0;
        this.createTimeStamp();
    }
    
    /**
     * Constructor for non file system events
     * @param newAction
     */
    public Task(TaskAction newAction){
        this.action=newAction;
        this.status = TaskStatus.WAITING;
        this.numProcessAttempts=0;
        this.createTimeStamp();
    }
    
    public Task(String filePathName, TaskAction newTaskAction,
            Date date) {
        this.action = newTaskAction;
        this.filePathName = filePathName;
        this.status = TaskStatus.WAITING;
        this.numProcessAttempts=0;
        this.date=date;
        this.createTimeStamp();
    }

    public boolean isFSEvent() {
        if (this.action == TaskAction.CREATE
                || this.action == TaskAction.DELETE
                || this.action == TaskAction.MODIFY
                || this.action == TaskAction.SCANALL) {
            return true;
        } else {
            return false;
        }
    }

    private void createTimeStamp(){
        timestamp=System.currentTimeMillis();
    }

    public long getTimestamp(){
        return timestamp;
    }

    @Override
    public String toString() {
        //return new String(this.action + ":::" + this.filePathName);
        return new String(this.action.toString());
    }

}
