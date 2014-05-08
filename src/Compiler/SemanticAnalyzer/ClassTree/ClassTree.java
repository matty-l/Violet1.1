package Compiler.SemanticAnalyzer.ClassTree;

import Compiler.SemanticAnalyzer.Util.BuiltInClassCompiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * This class is a data structure for holding information about classes hierarchy.
 * Created by Matt Levine on 4/26/14.
 */
public class ClassTree {

    private final ClassTreeNode root;
    private HashMap<String,ClassTreeNode> classMap = new HashMap<>();

    /** Constructs a new ClassTree
     */
    public ClassTree(){
        root = BuiltInClassCompiler.getObjectNode();
        classMap.putAll(BuiltInClassCompiler.getClassMap());
    }

    /** Returns true if the described method is contained by the given class
     * @param className name of class
     * @param methodName name of method
     * @param parameters method parameter type list
     * @return true if contained
     */
    public boolean containsMethod(String className, String methodName,
    /*String[]*/int parameters){
        String[][] params = new String[parameters][2];
        return classMap.containsKey(className) &&
            classMap.get(className).containsMethod(methodName, params);
    }

    /** Returns true if the described field is contained by the given class
     * @param className name of class
     * @param fieldName name of field
     * @param fieldType field type
     * @return true if contained
     */
    public boolean containsField(String className, String fieldName, String fieldType){
        return classMap.containsKey(className) &&
                classMap.get(className).containsField(fieldName, fieldType);
    }

    /** Applies the consumer to every class in the tree
     * @param action the consumer action
     */
    public void forEach(Consumer<? super ClassTreeNode> action){
        root.getAllDescendants().forEach(action);
    }

    /** Adds a class to the tree
     * @param node the class
     * @param parentName the name of the parent of the class
     * @throws MissingClassReferenceException no parent by the given name is known
     */
    public void addClassTreeNode(ClassTreeNode node, String parentName){
        if (!classMap.containsKey(parentName))
            throw new MissingClassReferenceException(parentName);
        ClassTreeNode parent = classMap.get(parentName);
        parent.addChild(node);
        node.setParent(parent);
        classMap.put(node.getName(),node);
    }

    /** Adds a method to the specified class, if the class is recognized
     * @param className the name of the class
     * @param methodName the name of the method
     * @param type the type of the method
     * @param formals the parameters of the method
     */
    public void addMethodToClass(final String className, final String methodName,
                                 final String type, final String[]... formals){
        if (classMap.containsKey(className))
            classMap.get(className).addMethod(methodName,type,formals);
    }

    /** Returns true if the given class is contained
     * @param className the name to validate
     * @return true if contained
     */
    public boolean containsClassEntry(String className){
        return classMap.containsKey(className);
    }

    /** Returns true if the class is defined and final, otherwise false.
     * @param parentName the name of the class
     * @return true if final and defined
     */
    public boolean classIsFinal(String parentName) {
        return classMap.containsKey(parentName) && classMap.get(parentName).isFinal();
    }

    /** Prints out all methods of the given class
     * @param className the name of the class
     */
    public void printMethods(String className){
        if (classMap.containsKey(className))
            classMap.get(className).printMethods();
    }

    /** Returns a list of name the methods of this class
     * @param className the name of class
     * @return list of methods
     */
    public List<String> getMethodsOfClass(String className) {
        if (classMap.containsKey(className)){
            return classMap.get(className).getMethods();
        }
        return new ArrayList<>();
    }

    /** Returns a list of name the fields of this class
     * @param className the name of class
     * @return list of fields
     */
    public List<String> getFieldsOfClass(String className) {
        if (classMap.containsKey(className)){
            return classMap.get(className).getFields();
        }
        return new ArrayList<>();
    }

    /** Adds a method to the specified class, if the class is recognized
     * @param className the name of the class
     * @param fieldName the name of the method
     * @param type the type of the method
     */
    public void addFieldToClass(final String className, final String fieldName,
                                 final String type){
        if (classMap.containsKey(className))
            classMap.get(className).addField(fieldName,type);
    }

    public class MissingClassReferenceException extends RuntimeException{
        public MissingClassReferenceException(String s){
            super("ClassTree Err: Cannot locate ClassTreeNode for referenced class "+s);
        }
    }

}


