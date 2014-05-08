package Compiler.Parser.ParserTree;

import Compiler.Parser.Matcher.State;
import Neuralizer.IO.NeuralLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/** Represents a node in an Parser.ParserTree
 * Created by Matt Levine on 3/13/14.
 */
public final class ParserTreeNode {
    final private ArrayList<ParserTreeNode> children;
    final public State value;

    /** Constructs a new ParserTreeNode
     * @param value the value of the node
     * @param children the children of the node
     */
    public ParserTreeNode(State value, ParserTreeNode... children){
        this.children = new ArrayList<>(Arrays.asList(children));
        this.value = value;
    }

    /** Constructs a ParserTreeNode from an Abstract Ndoe
     * @param node the abstract definition
     */
    public ParserTreeNode(ParserTreeNodeAbstraction node){
        this.value = node.value;
        this.children = new ArrayList<>(node.getChildren().size());
        for (ParserTreeNodeAbstraction child : node.getChildren()){
            children.add(new ParserTreeNode(child));
        }
    }

    /** Prints the node and its subnodes in a readable way
     * @param level the indentation
     */
    public void print(int level){
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < level; i++) s.append("   ");
        s.append(value.toString());
        NeuralLog.logMessage(s);
        for (ParserTreeNode child : children) child.print(level + 1);
    }

    /** Returns the number of nodes reachable from this node
     * @return number of reachable nodes
     */
    public int size(){
        int size = 1;
        for (ParserTreeNode child : children) size += child.size();
        return size;
    }

    /** Returns an iterator through the children of the TreeNode
     * @return an iterator through this node's children
     */
    public Iterator<ParserTreeNode> getChildren(){
        return children.iterator();
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

    /** Compares equality based on value and children
     * @param o the comparison object
     * @return true if equivalent
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ParserTreeNode treeNode = (ParserTreeNode) o;

        return children.equals(treeNode.children) &&
                !(value != null ? !value.equals(treeNode.value) : treeNode.value != null);

    }

    /** Computes hash code based on value and children
     * @return hash code
     */
    @Override
    public int hashCode() {
        int result = children.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
