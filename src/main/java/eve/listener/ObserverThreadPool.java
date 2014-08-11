
package eve.listener;

import eve.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ObserverThreadPool {

    private static final int NTHREDS = 1;
    private static ArrayList<String> directoryRegistry = new ArrayList<String>();
    private static ArrayList<Thread> observerThreads =  new ArrayList<Thread>();
    
    public static void registerDirectory(String pathName){
        File f = new File(pathName);
        if(f.exists() && f.isDirectory()){
            directoryRegistry.add(pathName);
        }else{
            Logger.info("Error HandlerThreadPool not valid listen path: " + pathName);
        }
    }

    public static void start() throws IOException{
        Logger.info("Observer ThreadPool Starting");
        
        //  Start a listener thread for each directory in directoryRegistry
        for (int i = 0; i < directoryRegistry.size(); i++) {
            Runnable listener = new ListenerThread(directoryRegistry.get(i));
            Thread listenerThread = new Thread(listener);
            listenerThread.start();
            observerThreads.add(listenerThread);
        }        
    }
    
    /**
     * HandlerThread needs to be interrupted in order for correct shutdown
     */
    public static void shutdown(){
        for(Thread t: observerThreads){
            t.interrupt();
        }
    }
    
    public static ArrayList<String> getDirs(){
        return directoryRegistry;
    }
    
    
    /**
     * Counts number of running threads and returns false if count>1
     * @return
     */
    public static boolean isTerminated(){
        int count=0;
        for(Thread t: observerThreads){
            if(t.isAlive()){
                count++;
            }
        }
        
        if(count>0){
            return false;
        }else{
            return true;
        }
    }
}
