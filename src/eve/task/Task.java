package eve.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;

public class Task implements Serializable{
	//
	//  variables
	//
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3339475349150893339L;
	public String filePathName;
	public TaskAction action;
	public TaskStatus status;
	public int numProcessAttempts;
	public Date date;
	
	public enum TaskStatus {
		WAITING, COMPLETE
	}
	
	/**
	 * Garbage collection purpose
	 */

	/**
	 * Constructor for file system events
	 */
	public Task(String newFilePathName, TaskAction newAction) {
		this.action = newAction;
		this.filePathName = newFilePathName;
		this.status = TaskStatus.WAITING;
		this.numProcessAttempts=0;
	}

	public Task(String newFilePathName, TaskAction newAction, TaskStatus s) {
		this.action = newAction;
		this.filePathName = newFilePathName;
		this.status = s;
		this.status = TaskStatus.WAITING;
		this.numProcessAttempts=0;
	}
	
	/**
	 * Constructor for non file system events
	 * @param newAction
	 */
	public Task(TaskAction newAction){
		this.action=newAction;
		this.status = TaskStatus.WAITING;
		this.numProcessAttempts=0;
	}
	
	public Task(String filePathName, TaskAction newTaskAction,
			Date date) {
		this.action = newTaskAction;
		this.filePathName = filePathName;
		this.status = TaskStatus.WAITING;
		this.numProcessAttempts=0;
		this.date=date;
	}

	//
	// Determine if task t is a filesystem event or not
	//
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

	//
	// happy printing functions
	//
	public void print(int indent) {
		for (int i = 0; i < indent; i++) {
			System.out.print(" ");
		}
		System.out.print(this.action + ":" + this.filePathName + "\n");
	}

	public void print() {
		System.out.print(this.action + ":" + this.filePathName + "\n");
	}

	@Override
	public String toString() {
		return new String(this.action + ":::" + this.filePathName);
	}

}
