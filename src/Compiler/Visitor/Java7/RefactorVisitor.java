package Compiler.Visitor.Java7;

import Compiler.SemanticAnalyzer.RawSyntaxTree;
import Compiler.SemanticAnalyzer.Util.ScopeTable;
import Compiler.Nodes.ASTNode;
import Compiler.Nodes.ASTNodeTypeJava7;
import Compiler.Parser.CFG.CFGToken;
import Compiler.Parser.ParserTree.ParserTreeNode;
import Compiler.Scanner.LexerToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * This class assists in refactoring by locating the tokens that match a given token
 * in value and scope.
 * Created by Matt Levine on 4/25/14.
 */
public class RefactorVisitor extends Java7Visitor {

    private HashSet<ParserTreeNode> refactorTokens = new HashSet<>();
    private HashMap<String,ASTNode> fields = new HashMap<>();
    private CFGToken base;
    private static final ASTNode NULLNODE = new ASTNode("null",null);
    private ASTNode baseASTNode = NULLNODE;
    private final RawSyntaxTree parseTree;
    int recursion = 0;
    private final ScopeTable scopes = new ScopeTable();

    public RefactorVisitor(RawSyntaxTree tree){
        parseTree = tree;
    }

    /** Returns the refactor tokens
     * @return the refactor tokens
     */
    public HashSet<ParserTreeNode> getRefactorTokens(){return refactorTokens;}

    /* Sets the base token
     * @param base the new base token
     */
    public void setBaseToken(LexerToken base){
        this.base = new CFGToken(base.getValue(),base.getValue(),
                base.getLineNum(),base.getColNum());
        parseTree.getRoot().accept(this);
        refactorTokens.add(baseASTNode.treeNode);
    }

    /* Visits type declaration */
    @Override
    public Object visitTypeDeclaration(ASTNode node){
        return scopeStatementSubroutine(node);
    }

    /** Visits class declaration */
    @Override
    public ArrayList<String> visitClassDeclaration(ASTNode node){
        node.getChildren().get(node.getNumChildren() - 1).accept(this);
        return null;
    }

    /* Visits field declaration */
    @Override
    public Object visitFieldDecl(ASTNode node){
        CFGToken token = node.getChildren().get(1).getChildren().get(
                0).treeNode.value.getEnd_chartRow().getCFGToken();
        fields.put(token.getValue(),node);

        //add to refactor list
        if (base.getValue().equals(token.getValue()) &&
                base.getColNum()==token.getColNum() && base.getLineNum()==token.getLineNum()) {
            baseASTNode = node;
            if (recursion == 0) {
                recursion++;
                parseTree.getRoot().accept(this);
                return null;
            }
        }
        //add to the class scope
        scopes.add(node.getChildren().get(1).getChildren().get(0).treeNode.getValue(), node);

        return null;
    }

    /** Go through methods and add matches here **/

    @Override
    public Object visitNewScopeMemberDecl(ASTNode node){
        scopes.incept();

        //handle method refactorizations
        for (ASTNode child : node.getChildren()){
            if (child.nodeType.equals(ASTNodeTypeJava7.Identifier)){
                ParserTreeNode treeNode = child.treeNode;
                CFGToken token = treeNode.value.getEnd_chartRow().getCFGToken();
                if (base.getValue().equals(token.getValue()) &&
                        base.getColNum()==token.getColNum() && base.getLineNum()==token.getLineNum()) {
                    baseASTNode = child;

                    boolean isRefactorable = nodesAreEqual((ASTNode) scopes.get(
                            token.getValue()), baseASTNode);
                    if (isRefactorable){
                        refactorTokens.add(child.treeNode);
                    }

                }
            }
            child.accept(this);
        }

        scopes.wakeUp();
        return null;
    }


    @Override
    public Object visitFormalParameterDecls(ASTNode node){

        ASTNode idNode = node.getChildren().get(node.getNumChildren()-1);
        if (node.getNumChildren() > 0 && idNode.getChildren().get(0).getNumChildren() > 0)
            scopes.add(
                idNode.getChildren().get(0).getChildren().get(0).treeNode.getValue(),idNode);

        return null;
    }

    @Override
    public Object visitVariableDeclarator(ASTNode node){
        ASTNode treeNode = (ASTNode) node.getChildren().get(0).accept(this);
        //check for duplicate declaration
        CFGToken token = treeNode.treeNode.value.getEnd_chartRow().getCFGToken();

        //add to refactor list
        if (base.getValue().equals(token.getValue()) &&
                base.getColNum()==token.getColNum() && base.getLineNum()==token.getLineNum()) {
            baseASTNode = node;
            if (recursion == 0) {
                recursion++;
                parseTree.getRoot().accept(this);
                return null;
            }

            boolean isRefactorable = nodesAreEqual((ASTNode) scopes.get(token.getValue()),
                    baseASTNode);
            if (isRefactorable){
                refactorTokens.add(treeNode.treeNode);
            }

        }
        scopes.add(treeNode.treeNode.getValue(), treeNode);

        for (ASTNode child : node.getChildren()) child.accept(this);

        return node;
    }

    @Override
    public Object visitExpression3(ASTNode node){
        int mark = -1;

        for (ASTNode child : node.getChildren()){
            mark--;
            //accept it
            Object o = child.accept(this);

            //check if we found a variable after a "this" token
            if (mark == 1 && o != null){
                ASTNode idNode = (ASTNode) ((ASTNode)o).accept(this);

                CFGToken token = idNode.treeNode.value.getEnd_chartRow().getCFGToken();

                if (base.getValue().equals(token.getValue()) &&
                        base.getColNum()==token.getColNum() &&
                        base.getLineNum() == token.getLineNum()) {
                    if (fields.containsKey(token.getValue()))
                        baseASTNode = fields.get(token.getValue());
                    else{
                        addOutcome(token.getLineNum(),"Cannot find field referenced by \"this\"");
                    }

                    if (recursion == 0) {
                        recursion++;
                        parseTree.getRoot().accept(this);
                        return null;
                    }
                }

                boolean isRefactorable = nodesAreEqual(baseASTNode,
                        fields.get(token.getValue()));

                if (isRefactorable){
                    refactorTokens.add(idNode.treeNode);
                }

            }
            //look for "this" tokens
            else if ( o != null &&  o.toString().equals("this")){
                mark = 2;
            }
        }
        return null;
    }

    @Override
    public Object visitPrimary(ASTNode node){
        //grab relevant values
        ASTNode posId = node.getChildren().get(0);
        if (posId.getChildren().size() > 0){
            CFGToken token = posId.treeNode.value.getEnd_chartRow().getCFGToken();

            if (baseASTNode == null && base.getValue().equals(token.getValue()) &&
                    base.getColNum()==token.getColNum() && base.getLineNum()==token.getLineNum()) {
                baseASTNode = (ASTNode) scopes.get(base.getValue());

                if (recursion == 0) {
                    recursion++;
                    parseTree.getRoot().accept(this);
                    return null;
                }
            }

            if (base.getValue().equals(token.getValue()) &&
                    base.getColNum()==token.getColNum() && base.getLineNum()==token.getLineNum()) {
                if (recursion == 0) {
                    recursion++;
                    parseTree.getRoot().accept(this);
                }
                return null;
            }

            boolean refactorable = nodesAreEqual((ASTNode) scopes.get(
                    posId.getChildren().get(0).treeNode.getValue()), baseASTNode);
            if ( refactorable ){
                refactorTokens.add(posId.treeNode);
            }
        }

        //pass the buck
        for (ASTNode child : node.getChildren()){
            child.accept(this);
        }

        return node.getChildren().get(0).accept(this);
    }

    /* Visits parenthetical expressions */
    @Override
    public Object visitParExpression(ASTNode node){
        for (ASTNode child : node.getChildren()){
            child.accept(this);
        }
        return node.getChildren().get(0).accept(this);
    }

    /* Visits selectors */
    @Override
    public ASTNode visitSelector(ASTNode node){
        for (ASTNode child : node.getChildren()) {
            child.accept(this);
        }

        if (node.getChildren().size() == 1)
            return node.getChildren().get(0);
        else return null;
    }

    /* Visits identifiers */
    @Override
    public ASTNode visitIdentifier(ASTNode node){
        return node.getChildren().get(0);
    }

    // -- Statements that require a scope change
    @Override public Object visitIfStatement(ASTNode node){
        return scopeStatementSubroutine(node); }
    @Override public Object visitElseStatement(ASTNode node){
        return scopeStatementSubroutine(node);}
    @Override public Object visitWhileStatement(ASTNode node){
        return scopeStatementSubroutine(node); }
    @Override public Object visitTryStatement(ASTNode node){
        return scopeStatementSubroutine(node);}
    @Override public Object visitFinallyStatement(ASTNode node){
        return scopeStatementSubroutine(node);}
    @Override public Object visitCatchStatement(ASTNode node){
        return scopeStatementSubroutine(node);}
    @Override public Object visitForStatement(ASTNode node){
        return scopeStatementSubroutine(node);}
    @Override public Object visitDoStatement(ASTNode node){
        return scopeStatementSubroutine(node);}

    private Object scopeStatementSubroutine(ASTNode node){
        scopes.incept();
        for (ASTNode child : node.getChildren()) child.accept(this);
        scopes.wakeUp();
        return null;
    }

    private static boolean nodesAreEqual(ASTNode n1, ASTNode n2){
        if (n1 == null || n2 == null ||
                n1.treeNode == null || n2.treeNode == null) return false;
        CFGToken tok1 = n1.treeNode.value.getEnd_chartRow().getCFGToken();
        CFGToken tok2 = n2.treeNode.value.getEnd_chartRow().getCFGToken();

        return tok1.getValue().equals(tok2.getValue()) && tok1.getLineNum()==tok2.getLineNum()
                &&tok1.getColNum()==tok2.getColNum();
    }

}