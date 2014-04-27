package Compiler.Visitor.Java7;

import Compiler.SemanticAnalyzer.RawSyntaxTree;
import Compiler.Nodes.ASTNode;
import GUI.Window.Utility.UtilWindow;
import javafx.scene.text.Text;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This Visitor gathers lists of classes from the import statements of
 * a program.
 * Created by Matt Levine on 4/6/14.
 */
public class ImportVisitor extends Java7Visitor{

    private LinkedList<String> classes;

    public String[] getImportedClassNames(RawSyntaxTree tree){
        classes = new LinkedList<>();
        /*try{
            classes.addAll(getLangLibrary());
        }catch(IOException | MissingLangDirectoryException e){
            throwError();
        }*/

        tree.getRoot().accept(this);
        return classes.toArray(new String[classes.size()]);
    }

    @Override
    public Object visitImportDeclaration(ASTNode node){
        //FIXME: fine for some purposes but not quite right
        //FIXME: grab methods and fields from these classes
        String className = node.getChildren().get(node.getNumChildren()-1).getChildren(
        ).get(0).treeNode.getValue();
        addClass(className);

        return null;
    }

    /** Internal subroutine for analyzing class information by class name **/
    private void addClass(String className){
        if (classes.contains(className)) return; // no need to waste time
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try{
            Class classObject = classLoader.loadClass(className);
            Method[] declaredMethods = classObject.getDeclaredMethods();
            Field[] declaredFields = classObject.getDeclaredFields();
        }catch (ClassNotFoundException c){
            System.out.println("Couldn't find class "+className);
        }
        classes.add(className);
    }



}

