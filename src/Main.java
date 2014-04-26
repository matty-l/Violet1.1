import GUI.DesktopController;
import GUI.Util.GuiLinkageManager;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.FileNotFoundException;
import java.util.stream.DoubleStream;

/**
 * Author: Matt
 * Date: 2/20/14
 * This class is designed to execute the Compiler with its GUI
 */
public class Main extends Application{

    public static void main(String[] args) throws FileNotFoundException {
//        PrintStream errorReDirection = new PrintStream("C:\\Users\\Matt Levine\\Desktop\\log.txt");
//        System.setOut(errorReDirection);

        System.out.println("Running...");
        launch(args);
        System.out.println("Program Terminated Safely.");
    }

    @Override
    /** Starts the GUI element **/
    public void start(Stage stage){
        DesktopController desktopController =
                new DesktopController(stage);
        new GuiLinkageManager(
                desktopController, stage);


    }

}