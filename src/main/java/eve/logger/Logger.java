package eve.logger;

import eve.Main;

public class Logger {
    private static Logger instance = null;

    public static void info(String msg){
        System.out.println("INFO:  "+msg);
    }

    public static void error(String msg){
        System.out.println("ERROR:  "+msg);
    }

    public static void debug(String msg){
        if(Boolean.parseBoolean(Main.config.getProperty("debug", "false"))){
            System.out.println("DEBUG:  "+msg);
        }
    }
}
