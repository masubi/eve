package eve;

import eve.handler.HandlerThreadPool;
import eve.listener.ObserverThreadPool;
import eve.logger.Logger;
import eve.task.ITaskQueue;
import eve.task.TaskQueue;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.util.Hashtable;
import java.util.Properties;

public class Main {
    public static String CONFIG_FILE = "config.properties";
    public static String DIRECTIVES_FILE = "directives.json";

    public static volatile boolean shutdownFlag = false;
    public static ITaskQueue taskQueue = new TaskQueue();

    //  handle user defined configs
    public static Properties config = new Properties();
    public static JSONArray directives = null;

    // k=targetDir, v=Directive
    public static Hashtable<String, JSONObject> directiveTable =new Hashtable<String, JSONObject>();

    public static void main(String[] args) {

        Main.loadPropertiesFile(CONFIG_FILE);
        Main.loadJSONFile(DIRECTIVES_FILE);

        try {
            ObserverThreadPool fsListener = new ObserverThreadPool();

            for(Object obj: directives){
                JSONObject targetDirective = (JSONObject) obj;
                fsListener.registerDirectory((String) targetDirective.get("targetDir"));
                directiveTable.put((String) targetDirective.get("targetDir"), targetDirective);
            }

            fsListener.start();

            HandlerThreadPool handlerThreadPool = new HandlerThreadPool();
            handlerThreadPool.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadPropertiesFile(String fileName) {
        try {
            Logger.info("Loading Configs: '"+fileName+"'");
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream is = classloader.getResourceAsStream(fileName);
            config.load(is);
        } catch (FileNotFoundException ex) {
            Logger.error("Problem loading config file: '" +fileName+"', error: "+ ex.toString());
            ex.printStackTrace();
        } catch (IOException e) {
            Logger.error("Problem loading config file: '" +fileName+"', error: "+ e.toString());
            e.printStackTrace();
        }
    }

    public static void loadJSONFile(String fileName){
        try {
            Logger.info("Loading Directives: '"+fileName+"'");
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            InputStream is = classloader.getResourceAsStream(fileName);
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(new InputStreamReader(is));
            JSONArray jsonArray = (JSONArray)obj;
            directives =jsonArray;

        }catch(Exception e){
            Logger.error("error loading directives file: " + e.toString());
        }
    }

}
