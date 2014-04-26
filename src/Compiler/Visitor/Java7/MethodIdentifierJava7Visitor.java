package Compiler.Visitor.Java7;

import Compiler.AbstractSyntaxTree.RawSyntaxTree;
import Compiler.AbstractSyntaxTree.Util.ScopeTable;
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

    public ArrayList<ParserTreeNode> getMethods(RawSyntaxTree tree){
        scopes = new ScopeTable();
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
        return defaultVisit(node);
    }

}
