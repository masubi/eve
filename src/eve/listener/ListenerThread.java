package eve.listener;

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
import eve.util.StopWatch;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;


public class ListenerThread implements Runnable {

	private final WatchService watcher;
	private final Map<WatchKey, Path> keys;
	private final boolean recursive = true;
	private boolean trace = false;

	public Mode mode = Mode.PROD;

	public enum Mode{
		TESTING, DEBUG, PROD
	}
	
	@SuppressWarnings("unchecked")
	static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	/**
	 * Register the given directory with the WatchService
	 */
	private void register(Path dir) throws IOException {
		WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE,
				ENTRY_MODIFY);

		if (trace) {
			Path prev = keys.get(key);
			if (prev == null) {
				System.out.format("register: %s\n", dir);
			} else {
				if (!dir.equals(prev)) {
					System.out.format("update: %s -> %s\n", prev, dir);
				}
			}
		}
		keys.put(key, dir);
	}

	/**
	 * Register a file tree this is necessary for windows implementation: BUG in
	 * jdk7 - recursive delete doesn't allow deletes of subdirectories see:
	 * http://stackoverflow.com/questions/6255463/java7-watchservice
	 * -access-denied-error-trying-to-delete-recursively-watched-ne
	 * 
	 * http://answerpot.com/showthread.php?1151653-WatchService%20-%
	 * 20Exposing%20More%20Of%20The%20Inotify%20Event%20Model/Page2
	 * 
	 * On *nix and mac recursive deletes are possible without problems
	 * 
	 * @param dir
	 * @throws java.io.IOException
	 */
	private void registerTree(Path dir) {
        WatchKey key = null;
        try {
            key = dir.register(watcher, new WatchEvent.Kind[]{ ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY},com.sun.nio.file.ExtendedWatchEventModifier.FILE_TREE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (trace) {
			Path prev = keys.get(key);
			if (prev == null) {
				System.out.format("register: %s\n", dir);
			} else {
				if (!dir.equals(prev)) {
					System.out.format("update: %s -> %s\n", prev, dir);
				}
			}
		}
		keys.put(key, dir);
	}


    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final Path start) throws IOException {
        // register directory and sub-directories
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException
            {
                register(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

	private void genTasksForFilesInDir(final Path start, final TaskAction ta) throws IOException {
		// register directory and sub-directories
		Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
		
			@Override
			public FileVisitResult visitFile(Path dir,
					BasicFileAttributes attrs) throws IOException {
				if(dir.toFile().isFile()){
					produceTask(new Task(dir.toString(), ta));
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}
	
	private void genTasksForFilesInDir(final Path start, final TaskAction ta, final Date date) throws IOException {
		// register directory and sub-directories
		Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
		
			@Override
			public FileVisitResult visitFile(Path dir,
					BasicFileAttributes attrs) throws IOException {
				if(dir.toFile().isFile()){
					produceTask(new Task(dir.toString(), ta, date));
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}
	
	/**
	 * Creates a WatchService and registers the given directory
	 */
	ListenerThread(String pathToRegister) throws IOException {
		Path dir = Paths.get(pathToRegister);
		this.watcher = FileSystems.getDefault().newWatchService();
		this.keys = new HashMap<WatchKey, Path>();

		// TODO: needs to handle permission denied
        StopWatch timer = new StopWatch(false);
        timer.start();
        System.out.format("Listener Scanning %s ...\n", dir);
        if (System.getProperty("os.name").startsWith("Windows")) {
            // includes: Windows 2000,  Windows 95, Windows 98, Windows NT, Windows Vista, Windows XP
            registerTree(dir);
        } else {
            registerAll(dir);
        }
        timer.printElapsedTimeSecs();
        timer.stop();

        // enable trace after initial registration
        this.trace = true;

		
	}

	private static TaskAction watchEventAsTaskAction(WatchEvent ev) {
		if (ev.kind() == ENTRY_MODIFY) {
			return TaskAction.MODIFY;
		}
		if (ev.kind() == ENTRY_DELETE) {
			return TaskAction.DELETE;
		}
		if (ev.kind() == ENTRY_CREATE) {
			return TaskAction.CREATE;
		}
		return null;
	}

	private boolean timerStarted=false;
	private final int MAX_EVENTS_BEFORE_SKIP=100;
	private final int TIMEINTERVAL=1000;
	private int count = 0;
	StopWatch eventTimer = new StopWatch(false);

	@Override
	public void run() {
		Logger.info("Listener Thread starting");

		
        while (Main.shutdownFlag == false) {

            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
                // key = watcher.poll(33, TimeUnit.MILLISECONDS);

            } catch (InterruptedException x) {
                System.out
                        .println("Listener Thread Shutdown Interrupt Recieved");
                return;
            }

            Path dir = keys.get(key);
            if (dir == null) {
                // System.err.println("WatchKey not recognized!!");
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind kind = event.kind();

                // if there are too many events in a short amount of time
                // then put a ScanAll in place
                count++;
                if (eventTimer.isRunning() == false)
                    eventTimer.start();

                if (eventTimer.getElapsedTime() < TIMEINTERVAL
                        && count > MAX_EVENTS_BEFORE_SKIP) {
                    Logger.info("TOO MANY EVENTS - skipping key");
                    Logger.info("timer("
                            + eventTimer.getElapsedTime() + "):count("
                            + count + ")");

                    count = 0;
                    eventTimer.restart();

                    key.reset();
                    // produceTask(new Task(TaskAction.SCANALL));
                    break;

                } else if (eventTimer.getElapsedTime() > TIMEINTERVAL) {
                    eventTimer.restart();
                    count = 0;
                }

                // TODO: handle OVERFLOW event
                if (kind == OVERFLOW) {
                    System.out
                            .println("ERROR:  Overflow event detected in Listener");
                    System.exit(0);
                    continue;
                }

                // Context for directory entry event is the file name of
                // entry
                WatchEvent<Path> ev = cast(event);
                Path name = ev.context();
                Path child = dir.resolve(name);

                if (this.mode == Mode.DEBUG || this.mode == Mode.TESTING) {
                    System.out.format("%s: %s\n", event.kind().name(),
                            child);
                }

                // TODO: handle symbolic links
                try {
                    // handle recursive create directory
                    if (Files.isDirectory(child, NOFOLLOW_LINKS)
                            && (kind == ENTRY_CREATE)) {
                        genTasksForFilesInDir(child, TaskAction.CREATE);

                        // delete directory
                        // if it is not a file then it must be a directory
                    } else if (!(new File(child.toString()).isFile())
                            && (kind == ENTRY_DELETE)) {

                        // recursively remove files from GlobalMetaData
                        produceTask(new Task(child.toString(),
                                TaskAction.DELETE_DIR));

                        // handle regular files
                    } else if (!Files.isDirectory(child, NOFOLLOW_LINKS)) {
                        if (new File(child.toString()).isFile()) {
                            TaskAction newTaskAction = watchEventAsTaskAction(ev);
                            String filePathName = child.toString();
                            produceTask(new Task(filePathName,
                                    newTaskAction));
                        }
                    }
                } catch (IOException e) {
                    Logger.error(e.toString());
                }

            }

            // reset key and remove from set if directory no longer
            // accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
        Logger.info("Listener Thread Shutdown");

	}
	
	

	
	/**
	 * Desc: Producer side of producers/consumers design pattern. Other
	 * producers are GC, CheckPoint, Restore Consumers is the
	 * MasterThreadManager thread
	 * 
	 */
	private void produceTask(Task taskToPush) {
        Logger.info(taskToPush.toString());
        Main.taskQueue.pushTask(taskToPush);
	}
}