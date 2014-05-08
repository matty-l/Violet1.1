package Compiler.Parser.ParserTree;

import Compiler.Parser.Matcher.State;
import Neuralizer.IO.NeuralLog;

import java.util.ArrayList;
import java.util.Arrays;

/** Represents a node in an ParserTree. As opposed to ParserTreeNode,
 * which is required for AbstractSyntaxTrees, this class is mutable.
 * This should not be necessary, but was having trouble with the
 * flaws in Earley's parse forest algorithm.
 * Created by Matt Levine on 3/13/14.
 */
public final class ParserTreeNodeAbstraction {
    private ArrayList<ParserTreeNodeAbstraction> children;
    public State value;

    /** Constructs a new ParserTreeNode
     * @param value the value of the node
     * @param children the children of the node
     */
    public ParserTreeNodeAbstraction(State value, ParserTreeNodeAbstraction... children){
        this.children = new ArrayList<>(Arrays.asList(children));
        this.value = value;
    }


    public void print(int level){
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < level; i++) s.append("   ");
        s.append(value.toString());
        NeuralLog.logMessage(s);
        for (ParserTreeNodeAbstraction child : children) child.print(level + 1);
    }

    public void addChild(ParserTreeNodeAbstraction node){
        children.add(node);
    }

    /** Returns  the children of the TreeNode
     * @return an iterator through this node's children
     */
    public ArrayList<ParserTreeNodeAbstraction> getChildren(){
        return children;
    }

    /** Returns the number of children directly stemming from this node
     * @return the number of children directly stemming from this node
     */
    public int getNumChildren(){return children.size();}

    /** Returns the value of the Node. May throw an exception if
     * the node is malformed... but I don't think it will...
     * @return the value of the node
     */
    public String getValue(){
        return value.getProduction().size() == 1 ? value.getProduction().get(0).getValue() : value.toString();
    }

    /** Returns the value of the node as a string (note: not as useful as
     * printing the node using <i>print</i>).
     * @return the value of the node as a string
     */
    @Override public String toString(){return value.toString();}

}
