package eve;

import eve.handler.HandlerThreadPool;
import eve.listener.ObserverThreadPool;
import eve.task.ITaskQueue;
import eve.task.TaskQueue;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class Main {

    public static Main instance = new Main();

    public static boolean shutdownFlag = false;
    public static ITaskQueue taskQueue = new TaskQueue();

    //  handle user defined configs
    public static Properties config = new Properties();
    public static final String propLoc = "config.properties";


    public static void main(String[] args) {

        //
        // Load system configurations
        //
        System.out.println("Loading system configuration from " + args[0]);
        Main.loadPropertiesFile(args[0]);

        //
        // Start file system listener and handler
        //
        try {
            ObserverThreadPool fsListener = new ObserverThreadPool();
            fsListener.registerDirectory(config.getProperty("targetDir"));
            fsListener.start();

            HandlerThreadPool handlerThreadPool = new HandlerThreadPool();
            handlerThreadPool.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadPropertiesFile(String filePathName) {
        try {
            config.load(new FileInputStream(filePathName));
        } catch (FileNotFoundException ex) {
            System.out.println("Problem loading config file");
            ex.printStackTrace();
        } catch (IOException e) {
            System.out.println("Problem loading config file");
            e.printStackTrace();
        }
    }
}
