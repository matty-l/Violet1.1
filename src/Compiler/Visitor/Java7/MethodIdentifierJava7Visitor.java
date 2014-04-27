package Compiler.Visitor.Java7;

import Compiler.SemanticAnalyzer.ClassTree.ClassTree;
import Compiler.SemanticAnalyzer.RawSyntaxTree;
import Compiler.SemanticAnalyzer.Util.ScopeTable;
import Compiler.Nodes.ASTNode;
import Compiler.Parser.ParserTree.ParserTreeNode;

import java.util.ArrayList;

/**
 * Created by Matt Levine on 4/26/14.
 */
public class MethodIdentifierJava7Visitor extends Java7Visitor {

    private ScopeTable scopes;
    private ArrayList<ParserTreeNode> methodNodes = new ArrayList<>();
    private ASTNode inMethod = null;
    private ClassTree classTree;

    public ArrayList<ParserTreeNode> getMethods(final RawSyntaxTree tree,
                                                final ClassTree classTree){
        scopes = new ScopeTable();
        this.classTree = classTree;
        tree.getRoot().accept(this);
        return methodNodes;
    }

    @Override
    public Object visitNewScopeMemberDecl(ASTNode node){
        scopes.incept();
        for (ASTNode child : node.getChildren()) {
            inMethod = child;
            child.accept(this);
        }
        scopes.wakeUp();
        return null;
    }

    @Override
    public Object visitIdentifier(ASTNode node){
        ASTNode id = (ASTNode) node.getChildren().get(0);
        if (inMethod != null){
            scopes.add(id.treeNode.value.getEnd_chartRow().getCFGToken().getValue(),
                    inMethod);

            inMethod = null;
        }
        return id.accept(this);
    }

    @Override
    public Object visitPrimary(ASTNode node){
        //grab relevant values
        ASTNode posId = node.getChildren().get(0);
        if (node.getNumChildren() == 1 ) handleVariablePrimaries(posId);
        else if (node.getNumChildren() == 2) handleDispatchPrimaries(posId);

        //pass the buck
        for (ASTNode child : node.getChildren()) child.accept(this);

        return node.getChildren().get(0).accept(this);
    }

    /** Handles variable-primaries */
    private void handleVariablePrimaries(ASTNode posId){
        //not implemented because this is a METHOD visitor
    }

    /** Handles dispatch primaries **/
    private void handleDispatchPrimaries(ASTNode posId){
        /*if (posId.getChildren().size() > 0){
            int level = scopes.indexOf(
                    posId.getChildren().get(0).treeNode.getValue());

            if (level == -1 && posId.getChildren().get(0).nodeType.toString().equals("ID")){
                ASTNode badNode = posId.getChildren().get(0);
                addOutcome(badNode.getAssociatedLineNum(), "Undeclared method " +
                        badNode.treeNode.getValue() + " at lne " +
                        badNode.getAssociatedLineNum());
                *//*System.out.println("Adding outcome: "+ "Undeclared method " +
                        badNode.treeNode.getValue() + " at lne " +
                        badNode.getAssociatedLineNum());*//*
            }
        }*/
    }

    @Override public Object visitType(ASTNode node){
        node.getChildren().get(0).accept(this);
        return null;
    }
      
     @Override public Object visitReferenceType(ASTNode node){
        ParserTreeNode refParse = node.getChildren().get(0).treeNode;
        String refName = refParse.value.getEnd_chartRow().getCFGToken().getValue();
         if (!classTree.containsClassEntry(refName)) {
             addOutcome(node.getAssociatedLineNum(), "Reference to " + refName + " is not recognized");
         }
        return node.getChildren().get(0).accept(this);
    }


}
