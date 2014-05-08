package Compiler.SemanticAnalyzer.Util;

import Compiler.SemanticAnalyzer.ClassTree.ClassTreeNode;
import GUI.Window.Utility.UtilWindow;
import IO.JavaCompiler;
import Neuralizer.IO.NeuralLog;
import javafx.scene.Scene;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class compiles information about the built-in classes
 * Created by Matt Levine on 4/26/14.
 */
public final class BuiltInClassCompiler {

    private static final LinkedList<ClassTreeNode> langClasses = new LinkedList<>();
    private static final HashMap<String,ClassTreeNode> classMap = new HashMap<>();

    private static final LinkedList<String> log = new LinkedList<>();
    private static ClassTreeNode OBJECT = null;

    static{
        try{
            langClasses.addAll(getLangLibrary());
            updateParentalControls();
        }catch (IOException | MissingLangDirectoryException | MissingJrtJarException e){
            throwError(e);
            OBJECT = new ClassTreeNode("Object", (Class) null,false);
            classMap.put("Object",OBJECT);
            ClassTreeNode STRING = new ClassTreeNode("String",(Class)null,false);
            classMap.put("String",STRING);
            STRING.setParent(OBJECT);
        }
    }

    /** Returns a list of messages that were logged at compilation
     * @return list of messages
     */
    public static Iterator<String> getLog(){ return log.listIterator(); }

    /** Returns the number of logged events in the BuiltInClass
     * @return number of logged events
     */
    public static int getNumLoggedEvents(){return log.size();}

    /** Returns the ClassTreeNode for the java.lang.Object Class
     * @return the Object ClassNode
     */
    public static ClassTreeNode getObjectNode(){ return OBJECT; }

    /** Returns a copy of the built-in classes' class-ap
     * @return a copy of the classmap
     */
    public static Map<String,ClassTreeNode> getClassMap(){
        return Collections.unmodifiableMap(classMap);
    }

    /** Sets the parents of each class in the directory - assumes the directory
     * is fully and correctly populated
     */
    private static void updateParentalControls(){
        if (OBJECT == null) throw new MissingObjectException();

        //add the rents
        for (ClassTreeNode classTreeNode : langClasses){
            Class classObj = classTreeNode.getClassDefinition();
            Class parent = classObj.getSuperclass();
            for (ClassTreeNode classTreeParent : langClasses){
                if (classTreeParent.getClassDefinition().equals(parent)){
                    classTreeNode.setParent(classTreeParent);
                    break;
                }
            }
            if (classTreeNode.getParent() == null && classTreeNode != OBJECT)
                classTreeNode.setParent(OBJECT);
        }

        //add the kids
        for (ClassTreeNode classTreeNode : langClasses){
            String name = classTreeNode.getName();
            classMap.put(name.substring(name.lastIndexOf(".")+1, name.length()),
                    classTreeNode);
            if (classTreeNode != OBJECT)
                classTreeNode.getParent().addChild(classTreeNode);
        }
    } //this method was really slow, but static so who cares

    /** Returns the classes automatically imported by the lang library
     * @return the classes automatically imported by the lang library
     */
    private static Set<ClassTreeNode> getLangLibrary() throws IOException{

        //Note -- this method is pretty inefficient, but static so ok

        Set<ClassTreeNode> classes = new HashSet<>();

        //get the lang library this cool way that I came up with
        String langDir = String.class.getResource("String.class").toString();
        langDir = langDir.replaceAll("%20"," ");
        if (langDir == null ) reportMissingLangDir();
        assert langDir != null;

        if (!langDir.contains("rt.jar")) throw new MissingJrtJarException();
        String langDirAbr =
                langDir.substring(langDir.indexOf("C:"),langDir.indexOf("rt.jar"))+"classlist";
        File langDirFile = new File(langDirAbr);

        BufferedReader fileContent = new BufferedReader(new FileReader(langDirFile));
        String line;
        StringBuilder assembly = new StringBuilder();
        //twice as much work to use two regexes, but significantly simpler logic
        Pattern class_ = Pattern.compile("lang/.*");
        Pattern classEnd = Pattern.compile("lang/[a-zA-Z]*");

        while ((line = fileContent.readLine()) != null){
            for (int i = 0; i < line.length(); i++){
                assembly.append(line.charAt(i));
                Matcher m = class_.matcher(assembly);
                Matcher m2 = classEnd.matcher(assembly);
                if (m.matches() && !m2.matches()){
                    addClass("java.lang."+assembly.subSequence(5,
                            assembly.length() - 1).toString().replaceAll("java", ""));
                    assembly.delete(0, assembly.length() - 1);
                }if (!m.hitEnd() && assembly.length() > 0){
                    assembly.delete(0,assembly.length()-1);
                }

            }
        }

        return classes;
    }

    /** Internal subroutine for analyzing class information by class name **/
    private static void addClass(String className){
        if (langClasses.contains(new ClassTreeNode(className, (Class) null,false)))
            return;

        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        ClassTreeNode class_ = null;
        try{
            Class classObject = classLoader.loadClass(className);
            Method[] declaredMethods = classObject.getMethods();
            Field[] declaredFields = classObject.getFields();
            boolean isFinal = classObject.getModifiers() == Modifier.FINAL;

            class_ = new ClassTreeNode(classObject.getName(),classObject,isFinal);
            for (Method m : declaredMethods) class_.addMethod(m);
            for (Field f : declaredFields) class_.addField(f);
            if (classObject.getName().equals("java.lang.Object"))
                OBJECT = class_;
        }catch (ClassNotFoundException c){
            log.add("Couldn't find class "+className);
        }
        if (class_ != null) langClasses.add(class_);
    }


    /** Displays a relevant error message **/
    private static void throwError(Exception e){
        NeuralLog.logError(e,Thread.currentThread());
        log.add(e.getMessage());
    }

    /** Reports a failure to load the java Lang directory **/
    private static void reportMissingLangDir(){
        throw new MissingLangDirectoryException();
    }

    public static void main(String[] args){
        for (ClassTreeNode kid : OBJECT.getAllDescendants()){
            System.out.println(kid);
        }
    }

}

class MissingLangDirectoryException extends RuntimeException{
    public MissingLangDirectoryException(){
        super("Import Err: Could not locate Lang directory");
    }
}

class MissingObjectException extends RuntimeException{
    public MissingObjectException(){
        super("Lang Initialization Err: Could not locate class \"Object\"");
    }
}

class MissingJrtJarException extends RuntimeException{
    public MissingJrtJarException(){
        super("Lang Initialization Err: Could not locate rt.jar");
    }
}