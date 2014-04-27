package Compiler.SemanticAnalyzer.ClassTree;

import Compiler.Nodes.ASTNode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * This class represents an individual class within
 * a class tree. This class is immutable (there is a non-critical field
 * that is mutable for convenience; see Bloch for a complete handling of
 * mutability in this case. Furthermore, there are fields which are
 * immutable locally but themselves not immutable structures - this too
 * is strictly for convenience and does not, with proper usage, affect
 * the reliability or stability of the class).
 * Created by Matt Levine on 4/26/14.
 */
public class ClassTreeNode {
    /** List of methods **/
    private final HashSet<ClassMethod> methods = new HashSet<>();
    /** List of fields **/
    private final ArrayList<ClassField> fields = new ArrayList<>();
    /** List of classes this one implements **/
    private final ArrayList<ClassTreeNode> implementedClasses = new ArrayList<>();
    /** The parent, or null if this class is object **/
    private ClassTreeNode parent;
    /** The name of the class **/
    private final String name;
    /** The children of this class **/
    private final ArrayList<ClassTreeNode> children = new ArrayList<>();
    /** The implementers of this class **/
    private final ArrayList<ClassTreeNode> implementers = new ArrayList<>();
    /** The definitional ASTNode, if the class is not built-in and this is relevant **/
    private final ASTNode definition_node;
    /** The definitional Class, if the class is built-in and is accessible. */
    private final Class definition_class;


    /** Constructs a new ClassTreeNode
     * @param name the name of the node
     * @param tree the definitional ASTNode
     */
    public ClassTreeNode(String name, ASTNode tree){
        this.name = name;
        this.definition_node = tree;
        definition_class = null;
    }

    /**
     * Constructs a new ClassTreeNode.
     * @param name the name of the node
     * @param class_ the definitional Class object
     */
    public ClassTreeNode(String name, Class class_){
        this.name = name;
        this.definition_node = null;
        this.definition_class = class_;
    }

    /** Sets the parent of this class to the given Class
     * @param class_ the parent class
     */
    public void setParent(ClassTreeNode class_){parent = class_;}

    /** Returns the ASTNode associated with this node **/
    public ASTNode getASTNode(){return definition_node;}

    /** Returns the Class object associated with this node **/
    public Class getClassDefinition(){return definition_class;}

    /** Adds a method to the class
     * @param method a method retrieved by reflection
     */
    public void addMethod(final Method method){
        String methodName = method.getName();
        String methodType = method.getReturnType().getTypeName();
        Parameter[] parameters = method.getParameters();
        String[][] parametersAsStrings = new String[parameters.length][2];

        for (int i = 0; i < parameters.length; i++){
            parametersAsStrings[i][0] = parameters[i].getType().getName();
            parametersAsStrings[i][1] = parameters[i].getName();
        }
        methods.add(new ClassMethod(methodName, methodType, parametersAsStrings));
    }

    /** Adds a method to the class
     * @param name the name of the node
     * @param type the type of the node
     * @param formals the list of formals
     */
    public void addMethod(final String name, final String type, final String[]... formals){
        methods.add(new ClassMethod(name, type, formals));
    }

    /** Adds a method to the class
     * @param name the name of the node
     * @param type the type of the node
     */
    public void addField(final String name, final String type){
        fields.add(new ClassField(name,type));
    }

    public void addField(Field field){
        String name = field.getName();
        String type = field.getType().getTypeName();
        fields.add(new ClassField(name,type));
    }

    /** Returns the name of the class
     * @return the name of the class
     */
    public String getName(){return name;}

    /** Returns the parent of the class
     * @return the parent of the class
     */
    public ClassTreeNode getParent(){return parent;}

    /** Returns true if the method is contained in the class
     * @param name the name of the method to return
     * @return the method object
     */
    public boolean containsMethod(final String name, final String type, final String[]... formals){
        return methods.contains(new ClassMethod(name, type, formals));
    }

    /** Returns true if the field is contained in the class
     * @param name the name of the field to return
     * @return the field object
     */
    public boolean containsField(final String name, final String type){
        return fields.contains(new ClassField(name, type));
    }

    /** Test the individual node class
     * @param args unused
     */
    public static void main(String[] args){
        ClassTreeNode node = new ClassTreeNode("Object", (ASTNode) null);
        String[][] formals = {{"int","myInt"},{"Object","obj"}};
        node.addMethod("foo","int",formals);
        node.addMethod("bar","Object",formals);
        node.addField("field1","int");

        System.out.println("True: "+node.containsMethod("foo","int",formals));
        System.out.println("True: "+node.containsField("field1","int"));
    }

    /** Two classes are equivalent if they have the same name
     * @param o comparison class
     * @return true if equivalent
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassTreeNode that = (ClassTreeNode) o;

        return name.equals(that.name);
    }

    /** Returns the hash by name
     * @return the hash
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /** Returns the number of methods defined.
     * @return number of fields
     */
    public int numMethods(){return methods.size();}

    /** Returns the number of fields defined.
     * @return number of fields
     */
    public int numFields(){return fields.size();}

    /** Returns the name of the parent or empty string if no par36ent is defined **/
    public String getParentName(){return parent == null ? "" : parent.name; }

    /** Returns a useful string representation of the node
     * @return string representation of node
     */
    @Override public String toString(){
        return "Class: [" +name+", "+getParentName()+", " +numMethods()+", "+numFields()+ "]";
    }

    /** Adds a child to the node
     * @param node the child
     */
    public void addChild(ClassTreeNode node){
        children.add(node);
    }

    /** Returns a copy of the children
     * @return a copy of the children
     */
    public List<ClassTreeNode> getChildrenUnmodifiable(){
        return Collections.unmodifiableList(children);
    }

    /** Returns all descendants of this node in a List, including this node. This
     * list has no particular guaranteed order (it will probably, but not
     * assuradely, return a list in the same order if called multiple times).
     * @return all descendants of this node, including this node
     */
    public List<ClassTreeNode> getAllDescendants(){
        Stack<ClassTreeNode> kidStack = new Stack<>();
        LinkedList<ClassTreeNode> descendants = new LinkedList<>();
        kidStack.add(this);

        while (!kidStack.isEmpty()){
            ClassTreeNode curNode = kidStack.pop();
            kidStack.addAll(curNode.getChildrenUnmodifiable());
            descendants.add(curNode);
        }
        return descendants;
    }
}
