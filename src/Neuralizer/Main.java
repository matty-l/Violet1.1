package Neuralizer;

import Neuralizer.IO.NeuralIOTrainer;
import Neuralizer.IO.NeuralLog;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Trains the Neuralizer.
 * Created by Matt Levine on 4/20/14.
 */
public class Main extends Application{

    private static boolean withInterface;
    private static int num_training_inputs;

    public static void main(String[] args){
        if (args.length != 2){
            throw new RuntimeException("Program requires exactly 1 boolean argument" +
                    " followed by exactly 1 integer argument.");
        }
        withInterface = Boolean.valueOf(args[0]);
        num_training_inputs = Integer.valueOf(args[1]);

       NeuralLog.logMessage("Neuralizing...");
        launch(args);
        NeuralLog.logMessage("Program Terminated Safely.");
    }

    @Override
    /** Starts the GUI element **/
    public void start(Stage stage){

        new NeuralIOTrainer(withInterface, num_training_inputs);

    }


}
