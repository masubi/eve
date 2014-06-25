package eve.task;

/**
 * CREATE = File system event for creating a file
 * MODIFY = File system event for modifying a file
 * DELETE  = File System even for deleting file
 * GC = Garbage Collection
 * GCCHECK = check if garbage collection needed
 * CHECKPOINT =  
 * RESTORE =  
 * NOACTION =  
 * SYNC = 
 * SHUTDOWN = 
 * SCANALL = scan all file and directories within the given directory 
 * and adds to MetaData
 * RESET = wipes out current metaData
 * CHECKPOINT_LOCAL = creates a local copy of metaData, does not affect
 * cloud MetaData
 * UPLOAD = this tells the MasterThreadMgr to spawn upload threads as necessary
 * LOAD_LOCAL = loads local metaData into GlobalMetaData
 * DELETE_DIR = recursively remove metaData from Dir in question.  This is
 * START_SIM = starts a simulation based on a trace
 * necessary to remove a shared memory problem
 * FLUSH_SEGMENT
 */
public enum TaskAction {
	CREATE, MODIFY, DELETE,
	NOACTION, SCANALL, DELETE_DIR
}
