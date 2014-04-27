package Compiler.Visitor.Java7;

import Compiler.Nodes.ASTNode;
import Compiler.Nodes.ASTNodeTypeJava7;
import Compiler.Parser.ParserTree.ParserTreeNode;
import Compiler.SemanticAnalyzer.ClassTree.ClassTree;
import Compiler.SemanticAnalyzer.RawSyntaxTree;
import Compiler.SemanticAnalyzer.Util.ScopeTable;

import java.util.ArrayList;

/**
 * This visitor identifies and handles dispatch statements. Currently,
 * it does not check the type of the parameters or the assignment of
 * a return.
 * Created by Matt Levine on 4/27/14.
 */
public class DispatchVisitor extends Java7Visitor{

    private ClassTree classTree;
    private ScopeTable scopes;
    private String curClass;

    public void getDispatches(final RawSyntaxTree tree,
                                                final ClassTree classTree){
        scopes = new ScopeTable();
        this.classTree = classTree;
        tree.getRoot().accept(this);
    }


    @Override
    public Object visitNewScopeMemberDecl(ASTNode node){
        scopes.incept();
        for (ASTNode child : node.getChildren()) child.accept(this);
        scopes.wakeUp();
        return null;
    }

    @Override public Object visitNormalClassDeclaration(ASTNode node){
        scopes.incept();
        curClass = ""+node.getChildren().get(0).accept(this);
        scopes.wakeUp();
        return defaultVisit(node);
    }

    @Override public Object visitEnumDeclaration(ASTNode node){
        scopes.incept();
        curClass = ""+node.getChildren().get(0).accept(this);
        scopes.incept();
        return defaultVisit(node);
    }

    @Override public Object visitInterfaceDeclaration(ASTNode node){
        scopes.wakeUp();
        curClass = ""+node.getChildren().get(0).accept(this);
        scopes.incept();
        return defaultVisit(node);
    }


    @Override
    public Object visitIdentifier(ASTNode node){
        return node.getChildren().get(0).accept(this);
    }

    @Override
    public Object visitPrimary(ASTNode node){
        //grab relevant values
        ASTNode posId = node.getChildren().get(0);
        if (node.getNumChildren() == 1 ) handleVariablePrimaries(posId);
        else if (node.getNumChildren() == 2) handleDispatchPrimaries(posId);

        //pass the buck
        for (ASTNode child : node.getChildren()) {
            child.accept(this);
        }

        return node.getChildren().get(0).accept(this);
    }

    @Override public Object visitVariableDeclaratorId(ASTNode node){
        return node.getChildren().get(0).accept(this);
    }

    /** Handles variable-primaries */
    private void handleVariablePrimaries(ASTNode posId){
        //not implemented because this is a METHOD visitor
    }

    /** Handles dispatch primaries **/
    private void handleDispatchPrimaries(ASTNode posId){
        System.out.println("handling dispatch primary");
        if (posId.getChildren().size() > 0){
            int level = scopes.indexOf(
                    posId.getChildren().get(0).treeNode.getValue());

            if (level == -1 && posId.getChildren().get(0).nodeType.toString().equals("ID")){
                ASTNode badNode = posId.getChildren().get(0);
                addOutcome(badNode.getAssociatedLineNum(), "Undeclared method " +
                        badNode.treeNode.getValue() + " at lne " +
                        badNode.getAssociatedLineNum());
                System.out.println("Adding outcome: "+ "Undeclared method " +
                        badNode.treeNode.getValue() + " at lne " +
                        badNode.getAssociatedLineNum());
            }
        }
    }

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

    @Override public Object visitDispatchNoReference(ASTNode node){
        if (node.getChildren().size() > 0) {
            String dispName = node.getChildren().get(0).getEnd_ColCFGToken().getValue();
            int numArgs = node.getChildren().get(1).getNumChildren();

            if (!classTree.containsMethod(curClass,dispName,numArgs)){
                addOutcome(node.getAssociatedLineNum(),
                        "Method "+dispName+ " with this signature is not in class "+
                                curClass);
            }
        }
        return null;
    }

    @Override public Object visitArguments(ASTNode node){
        defaultVisit(node);
        return node.getNumChildren();
    }

    @Override public Object visitSelector(ASTNode node){
        for (ASTNode child : node.getChildren()) {
            child.accept(this);
        }
        if (node.getNumChildren() > 1){
            String dispName = node.getChildren().get(0).getEnd_ColCFGToken().getValue();
            int numArgs = node.getChildren().get(1).getNumChildren();

            if (node.getProperty("thisClass").equals(true) &&
                    !classTree.containsMethod(curClass,dispName,numArgs)){
                addOutcome(node.getAssociatedLineNum(),
                        "Method "+dispName+ " with this signature is not in class "+
                                curClass);
            }
        }

        return node.getChildren().get(0).getEnd_ColCFGToken().getValue();
    }

    // -- Not Handling These Right Now
    @Override public Object visitExpression3(ASTNode node){
        int mark = -1;

        for (ASTNode child : node.getChildren()){
            mark--;
            //accept it

            child.setProperty("thisClass",false);
            Object o = child.accept(this);

            //check if we found a variable after a "this" token
            if (mark == 1 && o != null){
                child.setProperty("thisClass",true);
                child.accept(this);
            }
            //look for "this" tokens
            else if ( o != null &&  o.toString().equals("this")){
                mark = 2;
            }
        }
        return null;    }
}
