package Neuralizer.IO;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * This class stores log information.
 * Created by Matt Levine on 4/29/14.
 */
public class NeuralLog {

    /** If true, prints to normal output stream too;
     * threads can set freely, println is thread safe **/
    public static boolean debug = true;
    private static final PrintStream originalOutput = System.out;
    private static PrintStream outputLogger;
    static{
        try {
            outputLogger = new PrintStream("log.log");
        } catch (FileNotFoundException e) {
            throw new RuntimeException("IO Err: Cannot Load NeuralLog");
        }

        System.setOut(outputLogger);
        System.setErr(outputLogger);
    }

    static{
        System.out.println("Neural Log Loaded");
        System.out.println("-----------------");
    }

    /** Logs the given message in IO **/
    public static void logMessage(Object message){
        System.out.println(message);
        if (debug){
            System.setOut(originalOutput);
            System.out.println(message);
            System.setOut(outputLogger);
        }
    }

    /** Logs the error message in IO
     * @param throwable the error to throw
     * @param thread the thread throwing the error
     */
    public static void logError(Throwable throwable, Thread thread){
        System.out.println();
        System.out.println("Error generated from " +
                throwable.getClass().toString().replace("class","")
                + " on thread \"" + thread.getName() + "\": \"" +
                throwable.getMessage() + "\"");
        for (StackTraceElement stackTraceElement : throwable.getStackTrace()){
            System.out.println("\t"+stackTraceElement);
        }
        System.out.println();
    }

    public static void main(String[] args){

        for (int i = 0; i < 10; i++){
            NeuralLog.logMessage("hello world");
        }
        NeuralLog.logError(new RuntimeException("uh oh"),Thread.currentThread());
        NeuralLog.logMessage("hey");

    }

}
