package Compiler.Visitor.Java7;

import Compiler.Nodes.ASTNode;
import Compiler.Nodes.ASTNodeTypeJava7;
import Compiler.Parser.ParserTree.ParserTreeNode;
import Compiler.SemanticAnalyzer.ClassTree.ClassTree;
import Compiler.SemanticAnalyzer.ClassTree.ClassTreeNode;
import Compiler.SemanticAnalyzer.RawSyntaxTree;
import Compiler.SemanticAnalyzer.Util.ScopeTable;

import java.util.ArrayList;

/**
 * Created by Matt Levine on 4/27/14.
 */
public class ClassTreeBuilderVisitor extends Java7Visitor {

    private ClassTree classTree;

    /** Populates and returns a class tree based on a syntax tree
     * @param tree the syntax tree
     * @param classTree the class tree
     * @return the class tree
     */
    public ClassTree populateClassTree(final RawSyntaxTree tree,
                                                final ClassTree classTree){
        this.classTree = classTree;
        tree.getRoot().accept(this);
        return classTree;
    }

    @Override public Object visitClassOrInterfaceDeclaration(ASTNode node){
        Object nextIsFinal = false;
        for (ASTNode child : node.getChildren()){
            child.getChildren().get(0).setProperty("isFinal", nextIsFinal);
            nextIsFinal = child.accept(this);
        }
        return null;
    }

    @Override public Object visitModifier(ASTNode node){
        return node.getChildren().get(0).nodeType.equals(ASTNodeTypeJava7.FINAL);
    }


    @Override public Object visitNormalClassDeclaration(ASTNode node){
        ParserTreeNode nodeParse = node.getChildren().get(0).treeNode;
        String className = nodeParse.value.getEnd_chartRow().getCFGToken().getValue();

        String parentName = "Object";
        /* maybe make visitor process - would simply require making a separate
         * production, i.e. making NormalExtendingClassDecl different than
         * NormalClassDecl
         */
        if (node.getChildren().get(1).nodeType.equals(ASTNodeTypeJava7.EXTENDS)){
            parentName = ""+node.getChildren().get(2).accept(this);
        }
        try{
            classTree.addClassTreeNode(new ClassTreeNode(className,node,
                    node.getProperty("isFinal").equals(true)),parentName);
        }catch(ClassTree.MissingClassReferenceException e){
            //report if the parent isn't defined
            addOutcome(node.getAssociatedLineNum(),"Class "+parentName+" is not recognized");
        } 
        //report if the parent is final
        if (classTree.classIsFinal(parentName)) {
            addOutcome(node.getAssociatedLineNum(), "Class " + parentName + " is final");
        }

        return null;
    }

    /* We haven't implemented these yet */
    @Override public Object visitEnumDeclaration(ASTNode node){return null;}
    @Override public Object visitInterfaceDeclaration(ASTNode node){return null;}

    @Override public Object visitType(ASTNode node){
        return node.getChildren().get(0).accept(this);
    }

    @Override public Object visitReferenceType(ASTNode node){
        ParserTreeNode refParse = node.getChildren().get(0).treeNode;
        String refName = refParse.value.getEnd_chartRow().getCFGToken().getValue();

        if (!classTree.containsClassEntry(refName)) {
            addOutcome(node.getAssociatedLineNum(), "Reference to " + refName + " is not recognized");
        }
        return node.getChildren().get(0).accept(this);
    }

    @Override
    public Object visitPrimary(ASTNode node){
        //pass the buck
        for (ASTNode child : node.getChildren()) child.accept(this);
        return node.getChildren().get(0).accept(this);
    }

    @Override
    public Object visitIdentifier(ASTNode node){
        return node.getChildren().get(0).accept(this);
    }
}
