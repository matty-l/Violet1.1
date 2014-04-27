package GUI.Window;

import Compiler.Scanner.LexerToken;
import Compiler.SemanticAnalyzer.ClassTree.ClassTree;
import Compiler.SemanticAnalyzer.RawSyntaxTree;
import Compiler.Visitor.Java7.RefactorVisitor;
import GUI.Widget.SmartRichTextArea;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;

import java.util.ArrayList;
import java.util.List;

/**
 * This widget manages tab completion
 * Created by Matt Levine on 4/27/14.
 */
public class TabCompletionDialog {

    final ContextMenu popup = new ContextMenu();
    MenuItem[] textSuggestions;
    public final int number_text_suggestions = 4;
    final private double[] wordPosition = new double[2];

    private final SmartRichTextArea textArea;
    private LexerToken token;

    /** Constructs a new tab completion dialog
     * @param textArea a smart text are
     */
    public TabCompletionDialog(final SmartRichTextArea textArea){
        this.textArea = textArea;
        addMenuItems(number_text_suggestions);
    }

    /** Populates menu items
     * @param numItems number of items
     */
    private void addMenuItems(int numItems) {
        textSuggestions = new MenuItem[numItems];
        for (int i = 0; i < numItems; i++){
            textSuggestions[i] = new MenuItem("");
            popup.getItems().add(textSuggestions[i]);
        }
    }

    /** Displays suggestiosn
     * @param token the token
     * @param classTree the class tree
     * @param tree a syntax tree
     * @param classOfToken the name of token's class
     */
    public void show(final LexerToken token, ClassTree classTree, RawSyntaxTree tree,
                     String classOfToken){
        if (tree == null || classTree == null) return;
        this.token = token;
        wordPosition[0] = token.getColNum();
        wordPosition[1] = token.getLineNum();
        String[] favoriteWords = getPopularWords(token,classTree,tree,classOfToken);
        if (favoriteWords.length == 0) {
            popup.hide();
            return;
        }

        for (int i = 0; i < number_text_suggestions; i++){
            if ( !"".equals(favoriteWords[i]) && favoriteWords[i] != null){
                textSuggestions[i].setVisible(true);
                makeSuggestionWindow(favoriteWords[i], i);
            }else{
                textSuggestions[i].setVisible(false);
            }
        }
    }

    /** Returns suggestion-words
     * @param token the token
     * @param classTree the class tree
     * @param tree a syntax tree
     * @param classOfToken the name of token's class
     * @return list of suggestions
     */
    private String[] getPopularWords(final LexerToken token, ClassTree classTree,
                                     RawSyntaxTree tree, String classOfToken) {
        RefactorVisitor getScopeOfToken = new RefactorVisitor(tree);
        getScopeOfToken.setBaseToken(token);

        List<String> options = classTree.getMethodsOfClass(classOfToken);
        options.addAll(classTree.getFieldsOfClass(classOfToken));

        ArrayList<String> betterOptions = new ArrayList<>();
        int index = 0;
        for (String option : options){
            if (index >= number_text_suggestions) break;
            if (option.startsWith(token.getValue()) &&
                    !betterOptions.contains(token.getValue())) {
                betterOptions.add(option);
                index++;
            }
        }

        return betterOptions.subList(0,index).toArray(
                new String[number_text_suggestions]);
    }

    /** Shows suggestion window
     * @param word word to show from
     * @param indexPosition index of word placement
     */
    private void makeSuggestionWindow(final String word, final int indexPosition){
        textSuggestions[indexPosition].setText(word);
        textSuggestions[indexPosition].setOnAction(actionEvent -> completeTheWord(indexPosition));

        double[] xyPosition = getScreenXY(textArea);
        if (xyPosition == null) return;
        popup.show(textArea, Side.BOTTOM,xyPosition[0],xyPosition[1]);
        popup.setStyle("-fx-background-color: grey;");
        popup.setOpacity(0.65);
    }

    private double[] getScreenXY(TextArea textArea) {
        double fontsize = 11.5;
        double scrlY = textArea.getScrollTop()/1.008;
        double dScreenHeight = ((StackPane)textArea.getParent()).getHeight() - 664.0;
        return new double[]{
                (int) (-fontsize+0.9*((wordPosition[0]*fontsize/12.0+1)*8+22.5-
                        textArea.getScrollLeft()*1.11)),
                ( (StackPane)textArea.getParent()).getMaxHeight()
                        +(wordPosition[1]*fontsize*1.13)-620-scrlY-dScreenHeight //the ypos
        };
    }

    /** Hides the popup **/
    public void hide(){
        popup.hide();
    }

    /** Replaces the word in the text area **/
    public void completeTheWord(final int index){
        String word = textSuggestions[index].getText();
        if (word.equals("") || word == null) return;
        textArea.completeWord(new CompletionToken(token,word));
    }

    /** Token containing completion information **/
    public final class CompletionToken{
        private final LexerToken token;
        private final String replacementWord;

        /** Constructs a new token
         * @param token the lexer token to replace
         * @param replacementWord the word to replace it with
         */
        public CompletionToken(final LexerToken token, final String replacementWord){
            this.token = token;
            this.replacementWord = replacementWord;
        }

        /** Retursn the lexer token
         * @return hte token
         */
        public LexerToken getToken(){return token;}

        /** Return the replacement word
         * @return the replacement word
         */
        public String getReplacementString(){return replacementWord;}
    }

}
