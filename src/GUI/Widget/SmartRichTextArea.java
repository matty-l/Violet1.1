package GUI.Widget;

import Compiler.SemanticAnalyzer.ClassTree.ClassTree;
import Compiler.SemanticAnalyzer.ClassTreeDecorator;
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
import Compiler.Visitor.Java7.RefactorVisitor;
import Compiler.Visitor.VisitorToken;
import GUI.Util.SearchManager;
import GUI.Util.SearchToken;
import GUI.Window.TabCompletionDialog;
import GUI.Window.TabCompletionDialog.CompletionToken;

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

    private final TabCompletionDialog tabCompletionDialog = new TabCompletionDialog(this);



    private ArrayList<ParserTreeNode> fieldNodes = new ArrayList<>();
    private ClassTree classTree;

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
        for (int i = 0; i < fieldNodes.size(); i++){
            ParserTreeNode treeNode = fieldNodes.get(i);
            CFGToken tok = treeNode.value.getEnd_chartRow().getCFGToken();
            if (token.getValue().equals(tok.getValue()) && token.getColNum() == tok.getColNum() &&
                    tok.getLineNum() == token.getLineNum())
                        return true;
        }
        return false;
    }

    private boolean isCompletable(final LexerToken token){
        if (completionToken == null) return false;
        LexerToken compTok = completionToken.getToken();
        return compTok.getLineNum() == token.getLineNum() &&
                compTok.getColNum() == token.getColNum() &&
                token.getValue().equals(token.getValue());
    }

    /** Signals that this word was "this", so the next one should be marked **/
    private int nextWord = 0;
    /** Grabs the name of the next class (informal scope decision) **/
    private String nextClass = "";

    /** Returns the associated start tag for a keyword, else empty string.
     * @param token the token to modify
     * @param index the current index in the inputstream
     * @return stylization
     */
    @Override
    public String getStartModifier(LexerToken token, int index ){
        markClass(token);

        //getting the token at the current index
        if (index < markedCaretPosition ){
            yieldedToken = token;
        }

        int locale = getCaretPosition() - index - token.getValue().length();
//        System.out.println(token.getValue() + " -- " + locale);
        openCompletionDialog(token, locale == 0);

        if (attemptCompletion(token, index)) return "";

        if (attemptRefactor(token, index)) return "";

        //if something is searched for
        String searchResult = attemptSearch(token,getSearchToken(),index);
        if (!searchResult.equals("")) return searchResult;


        if (isField(token)){
            foundField = true;
            return  " <b><font color=\"#9900CC\"> ";
        }
        return token.style;
    }

    /** Marks the class - informal scope determination
     * @param token the token indicating class
     */
    private void markClass(LexerToken token) {
        if (token.getIds().equals(LexerToken.TokenIds.CLASS)) {
            nextClass = "-1";
        }
        else if (nextClass.equals("-1") && token.getIds().equals(LexerToken.TokenIds.ID)) {
            nextClass = token.getValue();
        }

    }

    private void openCompletionDialog(LexerToken token, boolean isSelected) {


        if (isSelected && token.getIds().equals(LexerToken.TokenIds.ID) || nextWord == 1) {
            tabCompletionDialog.show(token,classTree,lastTree,nextClass);
        }
        nextWord = token.getIds().equals(LexerToken.TokenIds.THIS) ? 3 : nextWord--;
    }

    private String attemptSearch(LexerToken token, SearchToken searchToken, int index) {
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
                    try{
                        replaceText(indexOfReplacement,
                                (indexOfReplacement + token.getValue().length()),
                                replacementString);
                    }catch(IndexOutOfBoundsException i){/*this is acceptable*/}
                return "";
            }

            return " <font style=\"background-color: #707070;\"> ";
        }
        return "";
    }

    private boolean attemptRefactor(LexerToken token, int index) {
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
            return true;
        }
        return false;
    }

    private boolean attemptCompletion(LexerToken token, int index) {
        if (isCompletable(token)){
            int indexOfReplacement = getText().indexOf(token.getValue(),index);

            if (indexOfReplacement+token.getValue().length() < getText().length() &&
                    indexOfReplacement+token.getValue().length() > 0 &&
                    indexOfReplacement > 0 && getText().substring(
                    index,index+token.getValue().length()).equals(token.getValue())) {

                replaceText(indexOfReplacement,
                        (indexOfReplacement + token.getValue().length()),
                        completionToken.getReplacementString());
            }
            return true;
        }
        return false;
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

    /** Decorates class trees **/
    private final ClassTreeDecorator classTreeDecorator = new ClassTreeDecorator();

    /** Handles the Raw Tree
     * @param buidler builder to assist in handling the tree
     */
    void handleRawAST(ASTBuilder buidler){
        Class type = classMap.get(getGrammar());
        lastTree = new RawSyntaxTree(buidler.getTreeHead(),type);
        classTree = new ClassTree();

        classTreeDecorator.decorate(lastTree,classTree,grammar);
        classTreeDecorator.transferOutcomes(getCatalog());
        classTreeDecorator.transferFields(fieldNodes);

    }

    /**Updates the formatting (see: RichTextArea) and marks unsaved**/
    @Override
    public void updateFormatting(){
        if (tabCompletionDialog != null) tabCompletionDialog.hide();
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

    private CompletionToken completionToken = null;

    public void completeWord(CompletionToken token) {
        completionToken = token;
        updateFormatting();
        completionToken = null;
        updateFormatting();
    }
}
