package GUI.Widget;

import Compiler.SemanticAnalyzer.ClassTree.ClassTree;
import Compiler.SemanticAnalyzer.RawSyntaxTree;
import Compiler.Nodes.ASTNodeTypeBantam;
import Compiler.Nodes.ASTNodeTypeJava7;
import Compiler.Parser.Builder.ASTBuilder;
import Compiler.Parser.CFG.CFGToken;
import Compiler.Parser.CFG.ContextFreeGrammar;
import Compiler.Parser.LanguageSource.BantamGrammarSource;
import Compiler.Parser.LanguageSource.JavaGrammar;
import Compiler.Parser.Matcher.Matcher;
import Compiler.Parser.ParserTree.ParserTreeNode;
import Compiler.Scanner.LexerToken;
import Compiler.Visitor.Bantam.FieldIdentifierBantamVisitor;
import Compiler.Visitor.Java7.FieldIdentifierJava7Visitor;
import Compiler.Visitor.Java7.ImportVisitor;
import Compiler.Visitor.Java7.MethodIdentifierJava7Visitor;
import Compiler.Visitor.Java7.RefactorVisitor;
import Compiler.Visitor.VisitorToken;
import GUI.Util.SearchManager;
import GUI.Util.SearchToken;

import java.util.*;

/**
 * Author: Matt
 * Date: 2/23/14
 * This class is a Rich Text Area that adds specialized features like
 * keyword recognition.
 */
public final class SmartRichTextArea extends RichTextArea {

    private boolean isSaved,foundField,foundSearch;
    private boolean threadNet = true;
    private ContextFreeGrammar grammar;
    private ASTBuilder lastBuilder;
    private String replaceText = "";
    private RawSyntaxTree lastTree = null;



    private ArrayList<ParserTreeNode> fieldNodes = new ArrayList<>();

    /** Constructs a new SmartRichTextArea **/
    public SmartRichTextArea(){
        super();
        isSaved = foundField = foundSearch = true;
        //default grammar is Java
        setGrammar(JavaGrammar.getJavaGrammar());
    }

    /** Returns true if the token is a field
     * @param token the token to verify
     * @return true if the token is matched
     */
    private boolean isField(LexerToken token){
        for (ParserTreeNode treeNode : fieldNodes){
            CFGToken tok = treeNode.value.getEnd_chartRow().getCFGToken();
            if (token.getValue().equals(tok.getValue()) && token.getColNum() == tok.getColNum() &&
                    tok.getLineNum() == token.getLineNum())
                        return true;
        }
        return false;
    }



    /** Returns the associated start tag for a keyword, else empty string. **/
    @Override
    public String getStartModifier(LexerToken token, int index ){

        //getting the token at the current index
        if (Math.abs(index - markedCaretPosition) < token.getValue().length() ){
            yieldedToken = token;
        }

        if (isRefactored(token)){
            int indexOfReplacement = getText().indexOf(token.getValue(),index);

            if (indexOfReplacement+token.getValue().length() < getText().length() &&
                    indexOfReplacement+token.getValue().length() > 0 &&
                    indexOfReplacement > 0 && getText().substring(
                    index,index+token.getValue().length()).equals(token.getValue())) {

                replaceText(indexOfReplacement,
                        (indexOfReplacement + token.getValue().length()),
                        replaceText);
            }
            return "";
        }

        //if something is searcched for
        if (SearchManager.isSearchedFor(token, getSearchToken())){
            foundSearch = false;
            //if we're replacing stuff, replace stuff
            if (!replaceText.equals("")){
                int indexOfReplacement = getText().indexOf(getSearchToken().phrase,index);
                String replacementString = replaceText;

                if (getSearchToken().type.name().contains("SINGLE")){
                    replaceText = "";
                    setSearchToken(null);
                }
                if (indexOfReplacement+token.getValue().length() < getText().length())
                    replaceText(indexOfReplacement,
                            (indexOfReplacement + token.getValue().length()),
                            replacementString);
                return "";
            }

            return " <font style=\"background-color: #707070;\"> ";
        }else if (isField(token)){
            foundField = true;
            return  " <b><font color=\"#9900CC\"> ";
        }
        return token.style;
    }

    private boolean isRefactored(LexerToken token) {
        for (ParserTreeNode node : refactorTokens){
            if (node == null)continue;
            CFGToken cfgToken = node.value.getEnd_chartRow().getCFGToken();
            if (token.getLineNum()==cfgToken.getLineNum()&&
                    token.getColNum()==cfgToken.getColNum()&&
                    token.getValue().equals(cfgToken.getValue())){
                return true;
            }
        }
        return false;
    }

    /** Returns the associated end tag for a keyword, else empty string. **/
    @Override
    public String getEndModifier(LexerToken token ){

        if (!foundSearch) {
            foundSearch = true;
            return "<font style=\"background-color: "+getBgColor()+";\">";
        }

        if (!token.style.equals("") || foundField){
            foundField = false;
            return " </font></b></i></a></u></s> ";
        }
        return "";

    }

    /** Sets whether the area is saved or unsaved
     * @param isSaved the flag indicating whether the area is saved
    **/
    public void setSaved(boolean isSaved){
        this.isSaved = isSaved;
    }

    /** Returns true iff the area has been saved
     * @return true iff the area has been saved
     */
    public boolean isSaved(){return isSaved;}

    private final static HashMap<ContextFreeGrammar,Class> classMap =
            new HashMap<ContextFreeGrammar, Class>(){{
       put(BantamGrammarSource.getBantamGrammar(), ASTNodeTypeBantam.class);
       put(JavaGrammar.getJavaGrammar(), ASTNodeTypeJava7.class);
    }};

    /** Handles the Raw Tree
     * @param buidler builder to assist in handling the tree
     */
    void handleRawAST(ASTBuilder buidler){
        Class type = classMap.get(getGrammar());
        lastTree = new RawSyntaxTree(buidler.getTreeHead(),type);

        //FIXME: make more elegant
        if (getGrammar().equals(BantamGrammarSource.getBantamGrammar())) {
            FieldIdentifierBantamVisitor visitor = new FieldIdentifierBantamVisitor();
            fieldNodes = visitor.getFields(lastTree);
            if (visitor.getOutcomes().size() > 0){
                getCatalog().addAll(visitor.getOutcomes());
            }
        }
        if (getGrammar().equals(JavaGrammar.getJavaGrammar())) {
            ClassTree classTree = new ClassTree(lastTree);

            FieldIdentifierJava7Visitor visitor = new FieldIdentifierJava7Visitor();
            MethodIdentifierJava7Visitor methodVisitor = new MethodIdentifierJava7Visitor();
            methodVisitor.getMethods(lastTree, classTree);
            fieldNodes = visitor.getFields(lastTree);
            getCatalog().addAll(visitor.getOutcomes());
            getCatalog().addAll(methodVisitor.getOutcomes());
        }


    }

    /**Updates the formatting (see: RichTextArea) and marks unsaved**/
    @Override
    public void updateFormatting(){
        super.updateFormatting();
        isSaved = false;
    }

    @Override protected void handleScannedTokens(){
        markSubroutineIncomplete();
        ASTBuilder builder = new ASTBuilder();

        if (threadNet) {
            threadNet = false;
            new Thread(() -> {
                Matcher m = getGrammar().matches(
                        getTokens().toArray(new LexerToken[getTokens().size()]), builder);

                if (m.matches() && getOverflowValue() == -1) {
                    handleRawAST(builder);
                    lastBuilder = builder; //primarily for testing
                } else if (!m.matches() && m.getBadToken() != null) {
                    getCatalog().add(new VisitorToken(m.getBadToken().getLineNum(),
                            "Parser Error: Cannot resolve token \"" +
                                    m.getBadToken().getValue() + "\" on line " +
                                    m.getBadToken().getLineNum() + " in column "
                                    + m.getBadToken().getColNum()));
                }
                markSubroutineComplete();
                threadNet = true;
            }).start();
        }else {
            return;
        }

        //Updates messages - FIXME add them all or more of them or something

        if (getCatalog().size() > 0){
            VisitorToken tok = getCatalog().remove(0);
            report(tok.lineNumber, tok.message);
            getCatalog().clear();
        }else {
            setErrorMessage("",-1);
        }

        //hack to get line numbers to reset and show the bad line
        getNumLines().set(getNumLines().get() + 1);
        getNumLines().set(getNumLines().get() - 1);
    }
    @Override
    /** Highlights all matching phrases
     * @param token the token to search for
     */
    public void find(SearchToken token){
        replaceText = "";
        super.find(token);
        //fixme: while this DOES update the scroll (sort of) it also causes the search to run twice
/*
        if (token != null && token.type.equals(SearchToken.SearchTokenType.SINGLE))
            if (selectionYield != null){
                double dY = selectionYield.getLineNum()*12 > getBoundsInParent().getMaxY() ?
                        getBoundsInParent().getMaxY() : selectionYield.getLineNum()*12;
                setScrollTop(dY);
            }
*/
    }


    /** Replaces with the given text the information in the search token
     * @param replaceText the text to replace
     * @param searchToken the token to search for
     */
    @Override
    public void replaceSelectionDynamic(String replaceText, SearchToken searchToken){
        this.replaceText = replaceText;
        super.find(searchToken);
    }


    /** The grammar to handleScannedTokens with **/
    public ContextFreeGrammar getGrammar() {
        return grammar;
    }

    /** Sets the grammer to the given grammar
     * @param grammar the new grammar
     */
    public void setGrammar(ContextFreeGrammar grammar) { this.grammar = grammar; }

    /** Caret position used for yielding marked tokens **/
    private int markedCaretPosition = -1;
    /** A Yielded Token **/
    private LexerToken yieldedToken = null;

    /** Returns the token of at the given index
     * @param indexOfClick the caret position in raw
     * @return token at that index
     */
    public LexerToken getTokenOfCurrentIndex(int indexOfClick) {
        markedCaretPosition = indexOfClick;
        updateFormatting();
        return yieldedToken;
    }

    /** Returns a new RefactorVisitor
     * @return a new refactor visitor
     */
    public RefactorVisitor getNewRefactorVisitor() {
        if (lastTree!=null){
            return new RefactorVisitor(lastTree);
        }
        return null;
    }

    private final HashSet<ParserTreeNode> refactorTokens = new HashSet<>();

    public void replaceSelectionRefactor(HashSet<ParserTreeNode> refactorTokens,
                                         final String replaceText) {
        this.refactorTokens.addAll(refactorTokens);
        this.replaceText = replaceText;
        updateFormatting();
        this.refactorTokens.clear();
        this.replaceText = "";
    }
}
