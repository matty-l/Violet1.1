package Compiler.Visitor.Java7;

import Compiler.AbstractSyntaxTree.RawSyntaxTree;
import Compiler.AbstractSyntaxTree.Util.ScopeTable;
import Compiler.Nodes.ASTNode;
import Compiler.Parser.CFG.CFGToken;
import Compiler.Parser.ParserTree.ParserTreeNode;
import Compiler.Scanner.LexerToken;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * This class assists in refactoring by locating the tokens that match a given token
 * in value and scope.
 * Created by Matt Levine on 4/25/14.
 */
public class RefactorVisitor extends Java7Visitor {

    private HashSet<ParserTreeNode> refactorTokens = new HashSet<>();
    private CFGToken base;
    private static final ASTNode NULLNODE = new ASTNode("null",null);
    private ASTNode baseASTNode = NULLNODE;
    private final RawSyntaxTree parseTree;
    int recurssion = 0;
    private final ScopeTable scopes = new ScopeTable();

    public RefactorVisitor(RawSyntaxTree tree){
        parseTree = tree;
    }

    /** Returns the refactor tokens
     * @return the refactor tokens
     */
    public HashSet<ParserTreeNode> getRefactorTokens(){return refactorTokens;}

    public void setBaseToken(LexerToken base){
        this.base = new CFGToken(base.getValue(),base.getValue(),
                base.getLineNum(),base.getColNum());
        parseTree.getRoot().accept(this);
        refactorTokens.add(baseASTNode.treeNode);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<String> visitCompilationUnit(ASTNode node){
        ArrayList<String> fields = new ArrayList<>();
        for (ASTNode child : node.getChildren()){
            Object o = child.accept(this);
            if (o instanceof ArrayList)
                fields.addAll((ArrayList<String>) child.accept(this));
        }
        return fields;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<String> visitTypeDeclaration(ASTNode node){
        ArrayList<String> fields = new ArrayList<>();
        scopes.incept();

        for (ASTNode child : node.getChildren()){
            Object o = child.accept(this);
            if (o instanceof ArrayList)
                fields.addAll((ArrayList<String>) o);
        }

        scopes.wakeUp();
        return fields;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<String> visitClassDeclaration(ASTNode node){
        ArrayList<String> fields = new ArrayList<>();

        fields.addAll((ArrayList<String>)
                node.getChildren().get(node.getNumChildren() - 1).accept(this));
        return fields;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<String> visitNormalClassDeclaration(ASTNode node){
        ArrayList<String> fields = new ArrayList<>();
        for ( int i = 0; i < node.getNumChildren(); i++){
            Object o = node.getChildren().get(i).accept(this);
            if (o instanceof ArrayList)
                fields.addAll((ArrayList)o);
        }
        return fields;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<String> visitMemberDecl(ASTNode node){
        ArrayList<String> fields = new ArrayList<>();
        for (ASTNode child : node.getChildren()){
            Object o = child.accept(this);
            if (o instanceof ArrayList) fields.addAll((ArrayList)o);
        }
        return fields;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<String> visitEnumDeclaration(ASTNode node){
        ArrayList<String> fields = new ArrayList<>();

        for (ASTNode child : node.getChildren()){
            Object o = child.accept(this);
            if (o instanceof ArrayList)
                fields.addAll((ArrayList<String>) child.accept(this));
        }
        return fields;
    }

    /** Grab the field names here **/

    @Override
    @SuppressWarnings("unchecked")
    public ArrayList<String> visitFieldDecl(ASTNode node){
        CFGToken token = node.getChildren().get(1).getChildren().get(
                0).treeNode.value.getEnd_chartRow().getCFGToken();
        //add to refactor list
        if (base.getValue().equals(token.getValue()) &&
                base.getColNum()==token.getColNum() && base.getLineNum()==token.getLineNum()) {
            baseASTNode = node;
            if (recurssion == 0) {
                recurssion++;
                parseTree.getRoot().accept(this);
                return null;
            }
        }
        //add to the class scope
        String name = node.getChildren().get(1).getChildren().get(0).treeNode.getValue();

        if (scopes.indexOf(name) == 1){
            int linenum = node.getChildren().get(1).getChildren().get(0
            ).treeNode.value.getEnd_chartRow().getCFGToken().getLineNum();
            addOutcome(linenum,"Duplicate field declaration of " +
                    name + " at line "+linenum);
        }
        scopes.add(node.getChildren().get(1).getChildren().get(0).treeNode.getValue(), node);

        //FIXME: wasteful data structures are being used
        return new ArrayList<String>(){{add(node.getChildren().get(1).treeNode.getValue());}};
    }

    /** Go through methods and add matches here **/

    @Override
    public Object visitNewScopeMemberDecl(ASTNode node){
        scopes.incept();
        for (ASTNode child : node.getChildren())
            child.accept(this);
        scopes.wakeUp();
        return null;
    }


/*    @Override
    public Object visitReferenceType(ASTNode node){
        return null;
    }*/

    @Override
    public Object visitMethodDeclaratorRest(ASTNode node){
        for (ASTNode child : node.getChildren()){
            child.accept(this);
        }
        return null;
    }

    @Override
    public Object visitFormalParameters(ASTNode node){
        for (ASTNode child : node.getChildren()){
            child.accept(this);
        }
        return null;
    }

    @Override
    public Object visitFormalParameterDecls(ASTNode node){
        ASTNode idNode = node.getChildren().get(1);
        scopes.add(idNode.getChildren().get(0).getChildren().get(0).treeNode.getValue(),idNode);
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
            if (recurssion == 0) {
                recurssion++;
                parseTree.getRoot().accept(this);
                return null;
            }

            boolean isRefactorable = baseASTNode.equals(scopes.get(token.getValue()));
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
                        base.getColNum()==token.getColNum() && base.getLineNum()==token.getLineNum()) {
                    baseASTNode = (ASTNode) scopes.get(base.getValue());
                    System.out.println("Defining in expr3");
                    if (recurssion == 0) {
                        recurssion++;
                        parseTree.getRoot().accept(this);
                        return null;
                    }
                }

                boolean isRefactorable = baseASTNode.equals(scopes.get(idNode.treeNode.getValue()));
                if (isRefactorable){
                    refactorTokens.add(idNode.treeNode);
                }

                /* scopes.isInScope(idNode.treeNode.getValue(),
                        scopes.indexOf(baseASTNode.getValue()));*/

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

            if (base.getValue().equals(token.getValue()) &&
                    base.getColNum()==token.getColNum() && base.getLineNum()==token.getLineNum()) {
                baseASTNode = (ASTNode) scopes.get(base.getValue());
                System.out.println("Defining in primary");
                if (recurssion == 0) {
                    recurssion++;
                    parseTree.getRoot().accept(this);
                    return null;
                }
            }

            if (base.getValue().equals(token.getValue()) &&
                    base.getColNum()==token.getColNum() && base.getLineNum()==token.getLineNum()) {
                if (recurssion == 0) {
                    recurssion++;
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

    @Override
    public Object visitParExpression(ASTNode node){
        for (ASTNode child : node.getChildren()){
            child.accept(this);
        }
        return node.getChildren().get(0).accept(this);
    }

    @Override
    public ASTNode visitSelector(ASTNode node){
        for (ASTNode child : node.getChildren()) {
            child.accept(this);
        }

        if (node.getChildren().size() == 1)
            return node.getChildren().get(0);
        else return null;
    }

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