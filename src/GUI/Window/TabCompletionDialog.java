package GUI.Window;

import Compiler.Scanner.LexerToken;
import GUI.Widget.SmartRichTextArea;
import javafx.geometry.Side;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Created by Matt Levine on 4/27/14.
 */
public class TabCompletionDialog {

    final ContextMenu popup = new ContextMenu();
    MenuItem[] textSuggestions;
    public final int number_text_suggestions = 4;
    final private double[] wordPosition = new double[2];

    private final SmartRichTextArea textArea;
    private LexerToken token;

    public TabCompletionDialog(final SmartRichTextArea textArea){
        this.textArea = textArea;
        addMenuItems(number_text_suggestions);
    }

    private void addMenuItems(int numItems) {
        textSuggestions = new MenuItem[numItems];
        for (int i = 0; i < numItems; i++){
            textSuggestions[i] = new MenuItem("");
            popup.getItems().add(textSuggestions[i]);
        }
    }

    public void makeWindow(String word, double xoffset,
                           double yoffset, int indexPosition){

    }

    public void show(final LexerToken token){
        this.token = token;
        wordPosition[0] = token.getColNum();
        wordPosition[1] = token.getLineNum();
        String[] favoriteWords = getPopularWords(token);

        for (int i = 0; i < number_text_suggestions; i++){
            if ( !"".equals(favoriteWords[i]) && favoriteWords != null){
                textSuggestions[i].setVisible(true);
                makeSuggestionWindow(favoriteWords[i],textArea.getCaretPosition(),0,i);
            }else{
                textSuggestions[i].setVisible(false);
            }
        }
    }

    private String[] getPopularWords(final LexerToken token) {
        return new String[]{"sug1","sug2","sug3","sug4"};
    }

    private void makeSuggestionWindow(final String word, final double xoffset,
                                      final double yoffset, final int indexPosition){
        textSuggestions[indexPosition].setText(word);
        textSuggestions[indexPosition].setOnAction(actionEvent -> {
            completeTheWord(indexPosition);
        });

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

    public void hide(){
        popup.hide();
    }

    public void completeTheWord(final int index){
        String word = textSuggestions[index].getText();
        textArea.completeWord(new CompletionToken(token,word));
    }

    public final class CompletionToken{
        private final LexerToken token;
        private final String replacementWord;

        public CompletionToken(final LexerToken token, final String replacementWord){
            this.token = token;
            this.replacementWord = replacementWord;
        }

        public LexerToken getToken(){return token;}
        public String getReplacementString(){return replacementWord;}
    }

}
