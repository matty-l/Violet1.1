package Neuralizer.IO;

import Neuralizer.Network.SelfOrganizingMap;
import Neuralizer.Structure.Matrix;
import Neuralizer.Network.NormalizeInput.NormalizationType;
import Neuralizer.Util.DifferenceVisualizer;
import Neuralizer.Util.TreeFlattenVisitor;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.DoubleStream;

/**
 * A titular reference to the Java "Preference" class, this class loads in a
 * saved SOM for future use, handling the exceptions and issues that might
 * occur within that process.
 * Created by Matt Levine on 4/29/14.
 */
public class NeuralPreferences extends Application{

    private SelfOrganizingMap selfOrganizingMap;

    /** Returns the network. Network must have been loaded first.
     * @return the netowrk
     */
    public SelfOrganizingMap getNetwork(){
        if (selfOrganizingMap == null)
            NeuralLog.logError(new RuntimeException("Must load SOM before returning it"),
                    Thread.currentThread());
        return selfOrganizingMap;
    }

    /** Attempts to load the SOM from memory
     */
    public void loadSOMFromMemory(){
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("brain.brn"));
        } catch (FileNotFoundException e) {
           NeuralLog.logError(e,Thread.currentThread());
        }

        int lineNum = -1;
        int width, height;
        Matrix output = null;

        if (reader != null){
            String s = "";
            try {
                while ((s=reader.readLine())!=null){
                    if (s.equals("")||s.equals("\n")) continue;
                    if (lineNum == -1){
                        width = Integer.valueOf(s.split("x")[0]);
                        height = Integer.valueOf(s.split("x")[1]);
                        output = new Matrix(width, height);
                        lineNum++;
                        continue;
                    }
                    s = s.replaceAll("\\|","").trim().replaceAll("  "," ");
                    String[] row = s.split(" ");
                    for (int i = 0; i < row.length; i++){
                        output.set(lineNum,i,Double.valueOf(row[i]));
                    }

                    lineNum++;
                }
                this.selfOrganizingMap = new SelfOrganizingMap(output, NormalizationType.MULTIPLICATIVE);
            } catch (IOException e) {
                NeuralLog.logError(e,Thread.currentThread());
            }
        }
    }

    /** For testing **/
    private static double[] tempInput;

    public static void main(String[] args){
        NeuralLog.logMessage("Neural Preferences Test");
        NeuralPreferences neuralPreferences = new NeuralPreferences();
        neuralPreferences.loadSOMFromMemory();

        double[] testSubject = new double[TreeFlattenVisitor.size_of_flattened_vector];
        testSubject = DoubleStream.of(testSubject).map(d->Math.random()).toArray();
        int win = neuralPreferences.getNetwork().winner(testSubject);
        double[] idealRow = neuralPreferences.getNetwork().getOutputWeights().getRow(win).toPackedArray();

        double[] differences = new double[testSubject.length];
        for (int i = 0; i < testSubject.length; i++) differences[i] = idealRow[i] - testSubject[i];
        differences = TreeFlattenVisitor.reverseFlattenArray(differences);
        double[][] wrapper = {DoubleStream.of(differences).map(d->d*d).toArray()};
        double magnitude = Math.sqrt(Matrix.MatrixMath.vectorLength(new Matrix(wrapper)));

        String s;
        for (int i = 0; i < testSubject.length;i++) {
            if (differences[i] < -30) s = "way too many";
            else if (differences[i] < -5) s = "slightly too many";
            else if (differences[i] > 5)  s = "slightly too few";
            else if (differences[i] > 30) s = "way too few";
            else s = "just the right number";
            NeuralLog.logMessage("You have "+s+" of "+
                    TreeFlattenVisitor.orderedStatementList[i] /*+ " has " +
                    differences[i]*/);
        }
        NeuralLog.logMessage("");

        NeuralLog.logMessage("The loaded matrix is: "+neuralPreferences.getNetwork().getOutputWeights());
        NeuralLog.logMessage("");
        NeuralLog.logMessage("The vector we're testing is: "+ Arrays.toString(testSubject));
        NeuralLog.logMessage("The result of the vector is: "+win);
        NeuralLog.logMessage("The \"ideal\" vector would have been: "+ Arrays.toString(idealRow));
        NeuralLog.logMessage("The difference array is: "+ Arrays.toString(differences));
        NeuralLog.logMessage("The magnitude of the error is "+magnitude);
        NeuralLog.logMessage("Done");

        tempInput = differences;
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        DifferenceVisualizer differenceVisualizer = new DifferenceVisualizer();
        differenceVisualizer.show();
        differenceVisualizer.displayVector(tempInput);
        differenceVisualizer.centerOnScreen();
    }
}
