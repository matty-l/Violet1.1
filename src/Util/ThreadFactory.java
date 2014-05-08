package Util;

import GUI.DesktopController;
import GUI.Widget.RichTextArea;

/**
 * This class facilitates Threading so that we don't start a bunch of
 * bad threads.
 * Created by Matt Levine on 4/28/14.
 */
public class ThreadFactory {

    private static RichTextArea textArea;
    private static final Thread alternateThread = new Thread(ThreadFactory::run);

    /** Runs the Thread's service **/
    private static void run(){
        while (!alternateThread.isInterrupted() && DesktopController.getFXThread().isAlive()){
            if (textArea != null){
                textArea.executeThreadServices();
                try {
                    Thread.sleep(DesktopController.getBackgroundDelay() * 1000 / 30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /** Forces the factory to update the text area by calling its
     * executeThreadServices method on a separate Thread. See that method's
     * description for details. This does not interrupt an executing service,
     * it only guarantees that the next Thread sweep will use the given
     * Text Area, has the TextArea not been set differently in the interim.
     * @param textArea the textarea to update
     */
    public static void forceUpdate(RichTextArea textArea) {
        ThreadFactory.textArea = textArea;
        if (!alternateThread.isAlive())
            alternateThread.start();
    }

    /** Interrupts the current Thread */
    public static void interrupt(){
        alternateThread.interrupt();
    }

}
