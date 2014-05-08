package Neuralizer.Util;

import GUI.Window.Utility.UtilWindow;
import Neuralizer.IO.NeuralLog;
import Neuralizer.Structure.Matrix;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.stream.DoubleStream;

/**
 * This class visualizes a difference vector.
 * Created by Matt Levine on 4/29/14.
 */
public class DifferenceVisualizer extends UtilWindow {


    public DifferenceVisualizer() {
        super("Code Statistics", 300, 500);
    }

    @Override
    protected void setCloseConditions() {}

    private ScrollPane mainPane;

    @Override
    public void addWidgets(){
        super.addWidgets();
        mainPane = new ScrollPane();
        mainPane.setPrefHeight(500);
        mainPane.setPrefWidth(300);
        root.setCenter(mainPane);
    }

    public void displayVector(double[] vector){
        if (vector.length != TreeFlattenVisitor.size_of_flattened_vector){
            NeuralLog.logError(new RuntimeException("Vector must be flattened size"),
                    Thread.currentThread());
        }
        BorderPane curPane = new BorderPane();
        mainPane.setContent(curPane);
        String s;
        Color color;

        for (int i = 0; i < vector.length; i++){
            Label name = new Label(TreeFlattenVisitor.orderedStatementList[i]);
            name.setFont(Font.font("Verdana", FontWeight.BOLD, 12));
            NeuralLog.logMessage(name+" " +vector[i]+" "+(vector[i]>25));

            if (vector[i] < -25) {
                s = "way too few";
                color = Color.CRIMSON;
            }
            else if (vector[i] < -5 && vector[i] > -25) {
                s = "too few";
                color = Color.LIGHTPINK;
            }
            else if (vector[i] > 5 && vector[i] < 25) {
                s = "slightly too many";
                color = Color.FORESTGREEN;
            }
            else if (vector[i] > 25) {
                s = "way too many";
                color = Color.GREEN;
            }
            else {
                s = "appropriate";
                color = Color.BLACK;
            }

            Label quantity = new Label(s);
            quantity.setTextFill(color);


            curPane.setLeft(name);
            curPane.setRight(quantity);
            BorderPane newPane = new BorderPane();
            curPane.setBottom(newPane);
            curPane = newPane;
        }

        Label name = new Label("Grade: ");
        Label grade = new Label(gradeVector(vector).toString());
        curPane.setLeft(name);
        curPane.setRight(grade);

    }

    public Grade gradeVector(double[] vector){
        if (vector.length != TreeFlattenVisitor.size_of_flattened_vector){
            NeuralLog.logError(new RuntimeException("Vector must be flattened size"),
                    Thread.currentThread());
        }

        double[][] wrapper = {DoubleStream.of(vector).map(d->d*d).toArray()};
        double magnitude = Math.sqrt(Matrix.MatrixMath.vectorLength(new Matrix(wrapper)));
        NeuralLog.logMessage(magnitude);

        if (magnitude < 25)
            return Grade.A;
        if (magnitude < 100)
            return Grade.B;
        if (magnitude < 200 )
            return Grade.C;
        if (magnitude < 500)
            return Grade.D;
        return Grade.F;

    }

    private static enum Grade {A,B,C,D,F}

}
