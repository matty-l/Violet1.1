package Compiler.Parser.Builder;

import Compiler.Parser.CFG.Termable;
import Compiler.Parser.Matcher.Chart;
import Compiler.Parser.Matcher.ChartRow;
import Compiler.Parser.Matcher.State;
import Compiler.Parser.ParserTree.ParserTreeNode;
import Neuralizer.IO.NeuralLog;

import java.util.*;

/**
 * Earley's original algorithm is flawed and Parsing Techniques is an n! complexity.
 * Went with "Parsing Inside-Out" which Stack Overflow referenced me to, it's
 * Joshua T. Goodman thesis from Harvard. He did a good job.

 * Created by Matt Levine on 5/1/14.
 */
public class ASTQuickBuilder implements Builder {

    private ParserTreeNode tree;
    private ArrayList<ArrayList<State>> states;


    /* Not implemented in this class
     * @param inputState not used
     * @return nothing
     * @throws BuildMethodNotImplemented because it isn't
     */
    @Override
    public ParserTreeNode build(State inputState) {
        NeuralLog.logMessage(inputState.name);
        Stack<State> stateStack = new Stack<State>(){{add(inputState);}};
        Stack<ChartRow> rows = new Stack<>();
        rows.add(inputState.getEnd_chartRow());

        while (!stateStack.isEmpty()){
            State state = stateStack.pop();
            ChartRow row = rows.pop();

            for (State posChild : row){
                if (posChild.completed())
                    NeuralLog.logMessage("State is complete: "+posChild+" and ");
//                if (posChild.completed())
//                    NeuralLog.logMessage("Querying "+posChild.name+
//                    " " +!posChild.equals(state)+" " +
//                            posChild.getStart_chartRow().equals(state.getStart_chartRow())+
//                    " contains: " +state.getProduction().contains(posChild)+" | " +state.name+" "
//                    +state.getProduction());
                if (posChild.completed() && !posChild.equals(state)
                        /*&& posChild.getStart_chartRow().equals(state.getStart_chartRow())*/
                        && state.getProduction().contains(posChild)) {

                            NeuralLog.logMessage("Accepting "+posChild.name+"->"+posChild.getProduction()+
                                   "     "+ posChild.name + " " +state.name+
                            " "+(posChild.getStart_chartRow() == posChild.getEnd_chartRow())
                            +" " +(posChild.getStart_chartRow() == state.getEnd_chartRow())
                            + " "+" || "+posChild.getEnd_chartRow()+
                            " ~~ "+posChild.getStart_chartRow());

                            stateStack.add(posChild);

                            rows.add(state.getEnd_chartRow());

                }
            }

        }


        System.exit(11);
        throw new BuildMethodNotImplemented();
    }

    /** Returns a built Parse tree
     * @param chart the input chart
     * @return ParseTree root node
     */
    @Override
    public ParserTreeNode build(Chart chart) {
        ArrayList<State> stateList = new ArrayList<>();
        HashSet<ChartRow> markedRows = new HashSet<>();
        HashSet<State> markedStates = new HashSet<>();

        Stack<ChartRow> rows = new Stack<>();
        rows.addAll(chart);

        while (!rows.isEmpty()){
            ChartRow row = rows.pop();
            //loop row
            for (State st : row){
                //if it's a completed state, grab it
                if (st.completed() && !markedStates.contains(st)) {
                    stateList.add(st);
                    NeuralLog.logMessage(st.name);
                    markedStates.add(st);
                }
                //if it links to rows we haven't considered, grab those too
                if (!markedRows.contains(st.getEnd_chartRow())) { //like the end row
                    rows.add(st.getEnd_chartRow());
                    markedRows.add(st.getEnd_chartRow());
                }
                if (!markedRows.contains(st.getStart_chartRow())) { //and the start row
                    rows.add(st.getStart_chartRow());
                    markedRows.add(st.getStart_chartRow());
                }
            }

        }

        NeuralLog.logMessage(stateList);
        NeuralLog.logMessage(stateList.size());
        NeuralLog.logMessage("Done with stuff");

        Stack<State> stateStack = new Stack<>();

        State inputState = null;
        for (State state : chart.get(chart.size()-1)){
            if (state.name.endsWith("GAMMA_RULE") && state.completed()){
                inputState = state;
                break;
            }
        }

        stateStack.add(inputState);
        markedStates.clear();
        int count =  0;

        while (!stateStack.isEmpty()){
            State state = stateStack.pop();

            for (Termable term : state.getProduction()){

                for (State st : stateList){
                    if (st.completed() && st.name.equals(term.getName()) && !markedStates.contains(st)){
                        //make sure we aren't mapping terminals to themselves repeatedly
                        if (!(st.getProduction().size()==1 && st.getProduction().get(0).getName().equals(st.name)))
                            stateStack.add(st);

                        markedStates.add(st);
                        NeuralLog.logMessage("adding "+st.name+
                                " "+ st.getProduction().size()+" "+
                                st.getEnd_chartRow().getCFGToken().getValue());
                        count++;

                        break;
                    }
                }

            }

        }

        NeuralLog.logMessage("done with all the stuff");
        NeuralLog.logMessage(count);

        System.exit(13);

        return null;
    }

    @Override
    public ParserTreeNode getTreeHead() {
        return tree;
    }
}
