package Compiler.SemanticAnalyzer;

import Compiler.Parser.CFG.ContextFreeGrammar;
import Compiler.Parser.LanguageSource.BantamGrammarSource;
import Compiler.Parser.LanguageSource.JavaGrammar;
import Compiler.Parser.ParserTree.ParserTreeNode;
import Compiler.SemanticAnalyzer.ClassTree.ClassTree;
import Compiler.Visitor.Bantam.FieldIdentifierBantamVisitor;
import Compiler.Visitor.Java7.ClassTreeBuilderVisitor;
import Compiler.Visitor.Java7.DispatchVisitor;
import Compiler.Visitor.Java7.FieldIdentifierJava7Visitor;
import Compiler.Visitor.Java7.MethodIdentifierJava7Visitor;
import Compiler.Visitor.VisitorToken;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class applies the appropriate visitors to validate
 * and populate a class tree
 * Created by Matt Levine on 4/27/14.
 */
public final class ClassTreeDecorator {

    /** Collection of fields **/
    private final ArrayList<ParserTreeNode> fieldNodes = new ArrayList<>();
    /** Collection of methods **/
    private final ArrayList<ParserTreeNode> methodNodes = new ArrayList<>();
    /** Visit results **/
    private final ObservableList<VisitorToken> catalog =
            FXCollections.observableArrayList();

    /** Static field visitor **/
    private static final FieldIdentifierJava7Visitor fieldVisitor =
            new FieldIdentifierJava7Visitor();
    /** Static method visitor **/
    private static final MethodIdentifierJava7Visitor methodVisitor =
            new MethodIdentifierJava7Visitor();
    /** Static class tree builder **/
    private static final ClassTreeBuilderVisitor builderVisitor =
            new ClassTreeBuilderVisitor();
    /** Static field visitor for bantam **/
    private static final FieldIdentifierBantamVisitor fieldVisitorBantam =
            new FieldIdentifierBantamVisitor();
    /** Static dispatch visitor **/
    private static final DispatchVisitor dispatchVisitor =
            new DispatchVisitor();

    /** Utility interface for lambda expression return types (not sure if this is the
     * perfect way to do this).
     */
    interface Executable {void go(RawSyntaxTree tree, ClassTree classTree);}


    private final HashMap<ContextFreeGrammar,Executable> grammarFunctionHashMap =
            new HashMap<ContextFreeGrammar,Executable>(){{
                put(JavaGrammar.getJavaGrammar(), (tree, classTree) ->
                {decorateJava7(tree,classTree);});
                put(BantamGrammarSource.getBantamGrammar(), (tree, classTree) ->
                {decorateBantam(tree, classTree);});
            }};

    /** Subroutine for decorating bantam **/
    private void decorateBantam(RawSyntaxTree tree, ClassTree classTree) {
        fieldVisitorBantam.getFields(tree);
        catalog.addAll(fieldVisitorBantam.popOutcomes());
    }

    /** Subroutine for decorating Java 7 **/
    private void decorateJava7(RawSyntaxTree tree, ClassTree classTree) {
        builderVisitor.populateClassTree(tree, classTree);
        catalog.addAll(builderVisitor.popOutcomes());
        fieldNodes.addAll(fieldVisitor.getFields(tree, classTree));
        catalog.addAll(fieldVisitor.popOutcomes());
        methodVisitor.getMethods(tree, classTree);
        catalog.addAll(methodVisitor.popOutcomes());
        dispatchVisitor.getDispatches(tree, classTree);
        catalog.addAll(dispatchVisitor.popOutcomes());
    }

    /** Decorates a syntax tree
     * @param classTree the class tree
     * @param tree the syntax tree
     * @param grammar the current CFG
     */
    public void decorate(RawSyntaxTree tree, ClassTree classTree,
                         ContextFreeGrammar grammar){
        grammarFunctionHashMap.get(grammar).go(tree,classTree);
    }

    /** Transfers any and all tokens to the given observable list (tokens internally
     * are flushed during transfer, as the name implies).
     * @param inputList destination to which to transfer
     */
    public void transferOutcomes(List<VisitorToken> inputList ){
        inputList.addAll(catalog);
        catalog.clear();
    }
      
  /** Transfers any and all fields to the given observable list (tokens internally
     * are flushed during transfer, as the name implies).
     * @param inputList destination to which to transfer
     */
    public void transferFields(List<ParserTreeNode> inputList ){
        inputList.addAll(fieldNodes);
        fieldNodes.clear();
    }

}
