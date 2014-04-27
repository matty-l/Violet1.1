package Compiler.Visitor.Java7;

import Compiler.SemanticAnalyzer.ClassTree.ClassTree;
import Compiler.SemanticAnalyzer.RawSyntaxTree;
import Compiler.SemanticAnalyzer.Util.ScopeTable;
import Compiler.Nodes.ASTNode;
import Compiler.Parser.ParserTree.ParserTreeNode;

import java.util.ArrayList;

/**
 * This visitor populates a class tree with the appropriate
 * methods and their signatures.
 * Created by Matt Levine on 4/26/14.
 */
public class MethodIdentifierJava7Visitor extends Java7Visitor {

    private ScopeTable scopes;
    private ArrayList<ParserTreeNode> methodNodes = new ArrayList<>();
    private ClassTree classTree;
    private String curClass = null;

    /* Returns arraylist of methods
     * @param tree a syntax tree
     * @param classTree a class tree
     */
    public ArrayList<ParserTreeNode> getMethods(final RawSyntaxTree tree,
                                                final ClassTree classTree){
        scopes = new ScopeTable();
        this.classTree = classTree;
        tree.getRoot().accept(this);
        return methodNodes;
    }

    /** Visits a new scope declaration */
    @Override
    public Object visitNewScopeMemberDecl(ASTNode node){
        scopes.incept();
        for (ASTNode child : node.getChildren()) child.accept(this);
        scopes.wakeUp();
        return null;
    }

    /** Visits a method declaration */
    @Override public Object visitMethodDecl(ASTNode node){
        classTree.addMethodToClass(curClass,
                "" + node.getChildren().get(1).getEnd_ColCFGToken().getValue(),
                "" + node.getChildren().get(0).accept(this),
                (String[][]) node.getChildren().get(2).accept(this));
        for (ASTNode child : node.getChildren()) child.accept(this);
        return null;
    }

    /** Visits the termianl part of a method declaration **/
    @Override public Object visitMethodDeclaratorRest(ASTNode node){
        for (ASTNode child : node.getChildren()) child.accept(this);
        if (node.getChildren().size() > 0) return node.getChildren().get(0).accept(this);
        return null;
    }

    /** Visits a formal parameter statement **/
    @Override public Object visitFormalParameters(ASTNode node){
        ArrayList<String[]> formals = new ArrayList<>();
        for (ASTNode child : node.getChildren()) {
            Object o = child.accept(this);
            if (o != null)
                formals.add((String[]) o);
        }
        return formals.toArray(new String[formals.size()][2]);
    }

    /** Visits a formal paramter declaration **/
    @Override public Object visitFormalParameterDecls(ASTNode node){
        int numDiscs = 0;
        String[] disc = new String[2];
        for (ASTNode child : node.getChildren()) {
            Object posDisc = child.accept(this);
            if (posDisc != null){
                disc[numDiscs] = "" + posDisc;
                numDiscs++;
            }
        }
        return disc;
    }

    /** Visits a normal class declaration **/
    @Override public Object visitNormalClassDeclaration(ASTNode node){
        scopes.incept();
        curClass = ""+node.getChildren().get(0).accept(this);
        scopes.wakeUp();
        return defaultVisit(node);
    }

    /** Visits an enumerated declaration **/
    @Override public Object visitEnumDeclaration(ASTNode node){
        scopes.incept();
        curClass = ""+node.getChildren().get(0).accept(this);
        scopes.incept();
        return defaultVisit(node);
    }

    /** Visits an interface declaration **/
    @Override public Object visitInterfaceDeclaration(ASTNode node){
        scopes.wakeUp();
        curClass = ""+node.getChildren().get(0).accept(this);
        scopes.incept();
        return defaultVisit(node);
    }

    /* Visits an ID declaration */
    @Override
    public Object visitIdentifier(ASTNode node){
        return node.getChildren().get(0).accept(this);
    }

    /* Visits a type declaration */
    @Override public Object visitType(ASTNode node){
        return node.getChildren().get(0).accept(this);
    }

    /* Visits a reference type declaration */
    @Override public Object visitReferenceType(ASTNode node){
        ParserTreeNode refParse = node.getChildren().get(0).treeNode;
        String refName = refParse.value.getEnd_chartRow().getCFGToken().getValue();

        if (!classTree.containsClassEntry(refName)) {
            addOutcome(node.getAssociatedLineNum(), "Reference to " + refName + " is not recognized");
        }
        return node.getChildren().get(0).accept(this);
    }

    /* Visits a primary declaration */
    @Override
    public Object visitPrimary(ASTNode node){
        //grab relevant values
        ASTNode posId = node.getChildren().get(0);
        //pass the buck
        for (ASTNode child : node.getChildren()) child.accept(this);

        return node.getChildren().get(0).accept(this);
    }

    /* Visits a variable decalration */
    @Override public Object visitVariableDeclaratorId(ASTNode node){
        return node.getChildren().get(0).accept(this);
    }


}
