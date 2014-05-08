package Neuralizer.IO;

import Compiler.Nodes.ASTNodeTypeJava7;
import Compiler.Parser.Builder.ASTBuilder;
import Compiler.Parser.CFG.ContextFreeGrammar;
import Compiler.Parser.LanguageSource.JavaGrammar;
import Compiler.Parser.Matcher.Matcher;
import Compiler.Parser.ParserTree.ParserTreeNode;
import Compiler.Scanner.LexerToken;
import Compiler.Scanner.Scanner;
import Neuralizer.Network.NormalizeInput;
import Neuralizer.Network.SelfOrganizingMap;
import Neuralizer.Network.TrainSelfOrganizingMap;
import Neuralizer.Structure.Matrix;
import Neuralizer.Structure.NeuralizerTree;
import Neuralizer.Util.NeuralErrorWin;
import javafx.animation.AnimationTimer;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Stack;
import java.util.stream.DoubleStream;

/**
 * This class builds the Neural network from built-in Java classes. Since this is a
 * use-once-and-dump program, its code is not intended to be remarkably beautiful,
 * but functional and easily managed.
 * Created by Matt Levine on 4/20/14.
 */
public class NeuralIOTrainer {

    private final String path = "C:\\Users\\Matt Levine\\Desktop\\Java Source";
    private ContextFreeGrammar grammar = JavaGrammar.getJavaGrammar();

    private final NeuralErrorWin errorWindow;
    private final SimpleBooleanProperty cflag = new SimpleBooleanProperty(true);
    private final SimpleBooleanProperty sflag = new SimpleBooleanProperty(false);
    private int step = 0;
    private long lastTime;
    private final long startTime;
    private boolean stop = false;

    private final ArrayList<Matrix> trainingSet = new ArrayList<>();
    private final int NUM_ITERATIONS = 100;
    private final int num_training_input;

    private PrintWriter output = null;

    /** This is where we write out output to **/
    {
        try {
             output = new PrintWriter("brain.brn","UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            report(Arrays.toString(e.getStackTrace()));
        }
    }

    /** Instantiates a new NeuralIOTrainer. The interface can be a little jumpy or buggy if
     * run over the entire program; it's used for debugging or getting a sense of
     * execution, not if you want to run the whole IO process.
     * @param withInterface if true, includes the interface
     * @param num_training_inputs the number of training inputs to terminate after
     */
    public NeuralIOTrainer(boolean withInterface, int num_training_inputs){
        if (withInterface)
            errorWindow = new NeuralErrorWin("Error",400,550,cflag,sflag);
        else errorWindow = null;
        this.num_training_input = num_training_inputs;
        startTime = System.nanoTime();

        report("Starting Neuralization");

        cflag.addListener((observableValue, aBoolean, aBoolean2) -> {
            if (aBoolean2) resume();
        });
        sflag.addListener((observableValue, aBoolean, aBoolean2)->{
            if (aBoolean2){
                SelfOrganizingMap map = null;
                TrainSelfOrganizingMap trainer = null;
                if (trainingSet.size() > 0) {

                    double[][] input = new double[trainingSet.get(0).size()][trainingSet.size()];
                    int nonNullSize = 0;

                    for (int i = 0; i < trainingSet.size(); i++){
                        if (DoubleStream.of(trainingSet.get(i).toPackedArray()).sum() != 0) {
                            if (i >= input.length)
                                break;
                            input[i] = trainingSet.get(i).toPackedArray();
                            nonNullSize++;
                        }
                    }

                    double[][] trainingInput = new double[nonNullSize][input[0].length];
                    System.arraycopy(input, 0, trainingInput, 0, nonNullSize);

                    map = new SelfOrganizingMap(input[0].length, nonNullSize,
                            NormalizeInput.NormalizationType.MULTIPLICATIVE);

                    trainer = new TrainSelfOrganizingMap(map,trainingInput,
                            TrainSelfOrganizingMap.LearningMethod.ADDITIVE,0.5);
                    trainer.initialize();

                    for (int i = 0; i < NUM_ITERATIONS; i++){
                        NeuralLog.logMessage("Reached iteration "+i+"...");
                        trainer.iteration();
                    }
                }
                stop = true;
                report("Scanning Stopped By User");
                if (output != null) {
                    NeuralLog.logMessage("Writing to file...");
                    output.println(map == null ? null : map.getOutputWeights().dim());
                    output.println(map == null ? null : map.getOutputWeights().toString());
                    output.close();
                    NeuralLog.logMessage("Done. Elapsed time: " + (System.nanoTime() - this.startTime) / 10e8);
                    System.exit(1);
                }
            }
        });

        File directory = new File(path);
        if (!directory.canRead() || !directory.isDirectory()){
            report("NeuralIOTrainer Instantiation Error (1" +
                    "): \n\tCannot " + "use NeuralIOTrainer without" + " local source directory");
            return;
        }


        new AnimationTimer(){

            @Override public void handle(long now) {
                if (stop) return;
                if ((now - lastTime)/0.5 >= 1000000000){
                    cflag.set(true);
                    lastTime = now;
                }
            }
        }.start();


        read(directory.listFiles());
    }

    /** Reads in a file from memory
     * @param input the file to read
     */
    private void read(File input){

        if (stop) return;
        report("Reading " + input.getName());
        String line;
        StringBuilder stringBuilder = null;
        if (input.isDirectory()) {
            read(input.listFiles());
        }
        else{
            stringBuilder = new StringBuilder();
            Charset charset = Charset.forName("US-ASCII");
            try{
                BufferedReader reader = Files.newBufferedReader(input.toPath(),charset);
                while((line = reader.readLine()) != null)
                    stringBuilder.append(line).append("\n");
            }catch (IOException ioe){
                report("Throwing IOException");
                report("NeuralIOTrainer Instantiation Error (2" +
                        "): \n\tCannot " + "use NeuralIOTrainer without" + " local source directory");
            }

            try {
                scanAndParse(stringBuilder, input.getName());
            } catch (InterruptedException e) {
                report("Err: Thread interruption in " + input.getName());
            }
        }
        cflag.set(false);
    }

    private Stack<File> unCheckedFiles = new Stack<>();

    private void read(File[] input){
        if (stop) return;
        report("Adding Directory of Size " + input.length);
        for ( File f : input ){
            if (cflag.get())
                read(f);
            else{
                unCheckedFiles.push(f);
            }
        }
    }

    private void resume(){
        if (stop) return;
        while(!unCheckedFiles.isEmpty()){
            if (cflag.get()){
                read(unCheckedFiles.pop());
            }else break;
        }
    }

    /**
     * Scans a parses a file by the given name
     * @param stringBuilder the builder in which to write information
     * @param filename the name of the file
     * @throws InterruptedException if the thread running the process is interrupted
     */
    private void scanAndParse(final StringBuilder stringBuilder, final String filename)
            throws InterruptedException {
        if (stop) return;
        report("Scanning " + filename + " : (" + stringBuilder.length() + ")");
        Scanner scanner = new Scanner(stringBuilder.toString(),false);
        ArrayList<LexerToken> tokens = new ArrayList<>();
        LexerToken token;

        while ((token = scanner.getNextToken()).getIds() != LexerToken.TokenIds.EOF){
            tokens.add(token);
        }
        ASTBuilder builder = new ASTBuilder();

        report("Building (1) " + filename);
        Matcher m;
        Thread t1 = new Thread(()->{
            threadedMatcherSubroutine(tokens, builder);
        });
        t1.start();
        final long startTime = System.nanoTime();
        while (t1.isAlive()){
            if (System.nanoTime()-startTime > 15e9){
                report("Thread Timeout: Aborted " + filename);
                NeuralLog.logMessage("Thread Err: Aborted " + filename);
                /* It's deprecated but I'm not going to get deadlock so not worried */
                t1.stop();
                return;
            }
            Thread.sleep(100);
        }

        report("Matched " + filename);
        if (builder.getTreeHead() != null){
            report("Building (2) " + filename);

            Thread t2 = new Thread(()->{buildNeurlizerTree(builder.getTreeHead(),ASTNodeTypeJava7.class);});
            t2.start();
            t2.join();
            if (stop) return;

            NeuralizerTree neuralizerTree = lastTree;
            Matrix flat = neuralizerTree.flatten();

            report("Parsed: " + step);
            double averageScanTime = (double)(System.nanoTime() - this.startTime) / 10e8 / step;
            NeuralLog.logMessage("Completed " + filename + " file #" + step +
                    ". Average Processing time: " + averageScanTime);
            trainingSet.add(flat);
            step++;
            if (step >= num_training_input) sflag.set(true);

            report(flat.toString());
        }else{
            NeuralLog.logMessage("Parser Err: Failed to Match: "+filename + " at token "+
                matcher.getBadToken().getValue() + " on line "+
                    matcher.getBadToken().getLineNum() + " at column " +
                    matcher.getBadToken().getColNum());
        }
    }

    private Matcher matcher;

    private void threadedMatcherSubroutine(ArrayList<LexerToken> tokens, ASTBuilder builder) {
        matcher = grammar.matches(
                tokens.toArray(new LexerToken[tokens.size()]), builder);
    }

    NeuralizerTree lastTree = null;

    /** Builds the neural tree
     * @param node the node of the tree
     * @param c the class to build
     */
    private void buildNeurlizerTree(ParserTreeNode node, Class c){
        if (stop) return;
        lastTree = new NeuralizerTree(node,c);
    }

    /** Reports somtheing to the interface, if in that mode
     * @param nErr the error to report
     */
    private void report(final String nErr){
        if (errorWindow != null) {
            errorWindow.mutate(nErr);
            errorWindow.show();
            errorWindow.centerOnScreen();
        }
    }

}
