package GUI.Widget;

import Compiler.Scanner.LexerToken;
import Compiler.Scanner.Scanner;
import Compiler.Visitor.VisitorToken;
import GUI.Util.SearchToken;
import com.sun.javafx.scene.web.skin.HTMLEditorSkin;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.web.HTMLEditor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Author: Matt
 * Date: 2/22/14
 * This class is designed to handle user text input. It allows for rich text
 * visualization with simple text input and transmission.
 */
public abstract class RichTextArea extends TextArea {

    private String fgColor = "white";
    private String bgColor  = "#101C2A";

    private final HTMLEditor outputStream;
    //flags used for linking caret positions
    private boolean toggleCaretValidtor;
    private boolean toggleTextValidator;
    private boolean toggleSelectionValidator;
    private boolean updated = false;
    private boolean subroutineComplete = true;

    //for handling errors
    private int lineOfBadToken = -1;
    private String messageOfBadToken = "";

    //List of current tokens in file
    private final ArrayList<LexerToken> tokens;

    private final SimpleIntegerProperty numLines;
    private final SimpleIntegerProperty caret;
    private SearchToken searchToken = null;

    private final ObservableList<VisitorToken> catalog =
            FXCollections.observableArrayList();

    //making sure symbols don't get converted to html
    private final HashMap<Character,String> htmlToRawTable = new HashMap<Character,String>(){{
        put('"',"&quot"); put('<',"&lt"); put('>',"&gt");  put('&',"&amp");
        put('?',"&#63");
    }};
    private int overFlowLine;

    public RichTextArea(){
        super();
        numLines = new SimpleIntegerProperty();
        caret = new SimpleIntegerProperty();
        toggleCaretValidtor = toggleSelectionValidator = true;
        outputStream = new HTMLEditor();
        tokens = new ArrayList<>();

        internal_config();
        linkTextAreas();
        updateFormatting();
    }

    /** Configures the area with stylistic parameters**/
    private void internal_config(){
        setOpacity(0.0);
        setWrapText(false);
        setStyle("-fx-font: 11.5px \"Courier New\"");
        hideToolBars();
    }

    /** Returns the property that holds the number of lines in the TextArea
     * @return The property holding the number of lines in the TextArea.
     */
    public IntegerProperty getNumLinesProperty(){ return getNumLines(); }

    /** Adds the TextArea to a given StackPane
     * @param tab The tab to which the Area is added.
     **/
    public void addToTab(Tab tab){
        //does this break FX convention?
        StackPane layer = new StackPane();
        layer.getChildren().addAll(outputStream,this);
        tab.setContent(layer);
    }

    /** Links the text from the raw TextArea to the htmlTextArea. Updates
     * caret position in front window on caret change in back window
     * and text change.
   **/
    private void linkTextAreas(){
        //some relatively complex logic to avoid both firing at once
        textProperty().addListener((observableValue, oldvalue, newValue) -> {
            //link fields
            if ( toggleTextValidator && !toggleCaretValidtor){
                toggleTextValidator = toggleSelectionValidator =false;
                updateFormatting();
                toggleTextValidator = true;
                toggleCaretValidtor = !toggleCaretValidtor;
            }
        });
        // --- on new selection
        selectionProperty().addListener((observableValue, indexRange, indexRange2) -> {
            if (toggleSelectionValidator)
                updateFormatting();
            toggleSelectionValidator = true;
        });
        // --- on any key release
        addEventFilter(KeyEvent.KEY_PRESSED, car -> {
            //handle special case characters
            String keyString = car.getCode().toString();
            //delete selection on key delete, otherwise runs after key stroke
            if (keyString.equals("BACK_SPACE") || keyString.equals("DELETE")) {
                Platform.runLater(this::updateFormatting);
            }

        });
        //on Scroll Change
        scrollLeftProperty().addListener((observableValue, number, number2) -> {
            updateFormatting();
        });
        scrollTopProperty().addListener((observableValue, number, number2) -> {
            updateFormatting();
        });

    }

    /** Hides the toolbars from the output **/
    private void hideToolBars(){
        outputStream.setSkin(new HTMLSkinWithoutToolbars(outputStream));
    }

    /** Replace HTML tokens
     * @return String the converted value
     * @param input the value to be converted
     */
    private String scan(String input){
        //init vars
        StringBuilder outputStream = new StringBuilder(input.length());
        int index, column;
        index = column = 0;
        Scanner scanner = new Scanner(input,true);
        LexerToken token;

        //add header
        outputStream.append(getHtmlHeader());
        getTokens().clear();
        overFlowLine = -1;

        //for one-space, {start string, open comment, and open big comment}
        boolean oneSpace = true;

        if (getCaretPosition() == 0){
            outputStream.append('|');
            getCaret().set(0);
        }

        //loop the stream and modify for HTML formatting
        while ( (token = scanner.getNextToken()).getIds() != LexerToken.TokenIds.EOF ){
            //grab next token, add to token list if appropriate
            String tokenString = token.getValue();
            if (token.getIds() != LexerToken.TokenIds.COMMENT && token.getIds()
                != LexerToken.TokenIds.NULL )
                    getTokens().add(token);

            outputStream.append(getStartModifier(token,index));

            for ( int r = 0; r < token.getValue().length(); r++ ){
                char c = tokenString.charAt(r);
                boolean terminalChar = ( c == ' ' || c == '\n' || c == '\t' );
                //validate individual words
                if (terminalChar && oneSpace ){
                    oneSpace = false;
                }else if (!terminalChar){
                    oneSpace = true;
                }
                else if (c != '\t') outputStream.append("&#8203");

                //catch selections and misc.
                if (htmlToRawTable.containsKey(c))
                    outputStream.append(htmlToRawTable.get(c));
                else if (c == '\t'){
                    for (int i =0; i < (column+8)/8*8-column-1; i++) outputStream.append("&nbsp");
                    outputStream.append(" ");
                    column += (column+8)/8*8-column - 1;
                }
                else outputStream.append(c);

                //handle caret
                if ( index == getCaretPosition()-1 ){
                    outputStream.append('|');
                    getCaret().set(column);
                }

                //handle selections
                handleSelections( outputStream, index );

                column = c != '\n' ? column + 1 : 0;
                index++;
            }
            outputStream.append(getEndModifier(token));
        }

        return outputStream.toString();
    }


    /** Calls the parser on the most recent scanned tokens, or whatever
     * else an extending class wants to do with scanned tokens. Might
     * typically involve updating error messages on the side panel.
    */
    protected void handleScannedTokens(){}

    /** Returns the header for the outputStream in html format **/
    private String getHtmlHeader(){
        String output = "";
        String[] xyScroll = getXYScroll();

        output += "<head><style type=\"text/css\"> " //head
                + "body{white-space:nowrap; font-size:11.5px; line-height: 125%; }"
                + "</style>"
                + "<script> function winScroll(){ window.scrollBy("
                + xyScroll[0] + "," + xyScroll[1] + "); } </script>" //adjust scroll
                + " </head>"
                + "<body bgcolor=\""+bgColor+"\" onload =\"winScroll()\">" //body
                + "<font color=\""+fgColor+"\">"
                + "<font face=\"Courier New\">"
                + "</body>";

        return output;
    }

    /**
     * Handles selection linkage (highlighting)
     * @param outputStream the stream to which to append linkage indicators
     * @param index the current index in the inputstream
     */
    private void handleSelections( StringBuilder outputStream, int index ){
        if (getSelection().getLength() == 0) return; //no selection
        //left selection
        if (getCaretPosition() <= getSelection().getStart()){
            if ( index == getCaretPosition()-1 || getCaretPosition() == 0 ){
                outputStream.append("<font style=\"background-color: #0893cf;\">");
            }
            else if ( index >= getSelection().getEnd() ){
                outputStream.append("<font style=\"background-color: #101C2A;\">");
            }
        }else{ //right selection
            if ( index == getSelection().getStart()-1){
                outputStream.append("<font style=\"background-color: #0893cf;\">");
            }
            else if ( index == getCaretPosition()-1 ||  getCaretPosition() == 0 ){
                outputStream.append("<font style=\"background-color: #101C2A;\">");
            }
        }
    }

    /** Returns a string representation of the XY sccroll amount of the text area
     * @return a two-item string array containing scroll left and scroll top respectively
     */
    public String[] getXYScroll(){
        return new String[]{String.valueOf(getScrollLeft()),
                String.valueOf(getScrollTop()) };
    }

    /** Subroutine for linking areas on change **/
    private void linkAreasSubroutine(){
        //link fields
            String inputStream = getText();
                //step 1 of compilation
                inputStream = scan(inputStream).replace(
                        "\n", "<br>");
                outputStream.setHtmlText(inputStream);

        getNumLines().set(getText().split("\n", -1).length);
    }

    /** The beginning sequence of a word modification for a given word. This class does
     * not implement this method; it is not abstract because it is not required by
     * subclasses, rather they may implement it at their leisure.
     * @param token the token to modify
     * @param index the current index in the inputstream
     * @return the HTML modifier
     */
    public String getStartModifier( LexerToken token,int index ){ return ""; }

    /** The ending sequence of a word modification for a given word. This class does
     * not implement this method; it is not abstract because it is not required by
     * subclasses, rather they may implement it at their leisure.
     * @param token the token to modify
     * @return the HTML modifier closure
     */
    public String getEndModifier( LexerToken token ){ return ""; }

    /** Attempts to update formatting on the textArea. This method can be overwritten
     * to allow for particular actions on changes to the text area, but a call to super
     * should be made every time.
    **/
    public void updateFormatting(){
        updated = false;
        linkAreasSubroutine();
    }

    /** Marks the area as not updated. Must be called by routines
     * to signal that handleScannedTokens be reprocessed, or else
     * handleScannedTokens will never execute
    **/
    protected final void markSubroutineComplete(){subroutineComplete = true;}

    /** Marks the area as updated. Must be called by routines
     * to signal that handleScannedTokens not be processed, usually if
     * the processing is still ongoing and overlap with separate threads
     * is unwelcome.
     **/
    protected final void markSubroutineIncomplete(){subroutineComplete = false;}


    /** Forces the area to update itself with all supplementary functionalities **/
    public final void forceUpdate(){
        if (!updated && subroutineComplete)
            handleScannedTokens();
        updated = true;
    }

    /** Returns true iff the text is empty **/
    public boolean isEmpty(){return getText().equals(""); }

    /** Returns the index of the last bad token
     * @return the index of the last bad token
     */
    public final int getLineOfBadToken(){return lineOfBadToken;}

    /** Returns a description of the last bad token
     * @return the description of the last bad token
     */
    public final String getMessageOfBadToken(){return messageOfBadToken;}

    /** Reports a token error to  the RichTextRegion; not guaranteed to do anything.
     * @param lineNum the line number of the error
     * @param message the message associated with the error
     */
    public final void report(int lineNum, String message){
        lineOfBadToken = lineNum;
        messageOfBadToken = message;
        getNumLines().set(getNumLines().get() + 1);
        getNumLines().set(getNumLines().get() - 1);
    }

    /** Highlights all matching phrases
     * @param token the token to search for
     */
    public void find(final SearchToken token){
        setSearchToken(token);
        updateFormatting();
     }

    /** Sets the foreground color
     * @param style the new style
     */
    public final void setForegroundColor(final String style){ fgColor = style; }

    /** Sets the foreground color
     * @param style the new style
     */
    public final void setBackgroundColor(final String style){ bgColor = style; }

    /** Dynamically implemented replacement functionality. Not implemented in this class,
     * not abstract because is not required for children; children may implement at their
     * leisure.
     * @param replacement the string to replace
     * @param searchToken the token to search for
     */
    public void replaceSelectionDynamic(String replacement, SearchToken searchToken){}

    /** Returns the overflow line value
     * @return overflow line value
     */
    protected final int getOverflowValue(){return overFlowLine;}

    /** Sets the message and line index for displaying an error
     * @param message the error message
     * @param lineNum the line number of the error
     */
    protected final void setErrorMessage(final String message, final int lineNum){
        messageOfBadToken = message;
        lineOfBadToken = lineNum;
    }

    /** Returns the background color
     * @return the background color
     */
    protected final String getBgColor(){return bgColor;}

    /** Returns the foreground color
     * @return the foreground color
     */
    protected final String getFgColor(){/*Provided for completeness*/return fgColor;}

    /** Returns the actual list of internal tokens. Please don't
     * break them, or the widget might not work properly for a very little
     * bit (you can't do too much damage).
     * @return  the internal tokens
     */
    protected final List<LexerToken> getTokens() {
        return tokens;
    }

    /** Returns The number of lines in the area
     * @return the number of lines in the area
     */
    protected final SimpleIntegerProperty getNumLines() { return numLines; }

    /** Returns the column of the caret
     * @return the column of the caret
     */
    public final SimpleIntegerProperty getCaret() {  return caret;  }

    /** Returns the word to search for
     * @return the word to search for
     */
    protected final SearchToken getSearchToken() { return searchToken; }

    /** Sets the search token to the given token
     * @param searchToken the token to search for
     */
    protected final void setSearchToken(SearchToken searchToken) {
        this.searchToken = searchToken;
    }

    /** Allows retrieval of messages from visitors on separate threads
     * (I don't like this) **/
    public List<VisitorToken> getCatalog() {
        return catalog;
    }

}

/** This internal class is an HTMLEditor without the toolbars
 */
class HTMLSkinWithoutToolbars extends HTMLEditorSkin{

    private final GridPane grid = (GridPane) getChildren().get(0);

    public HTMLSkinWithoutToolbars(HTMLEditor htmlEditor) throws NullPointerException{
        super(htmlEditor);
        //remove the toolbars
        ((ToolBar)grid.getChildren().get(0)).setMinHeight(0);
        ((ToolBar)grid.getChildren().get(1)).setMinHeight(0);
    }

    @Override
    protected void layoutChildren(final double x, final double y, final double w,
                                  final double h){
        //we've removed the call to build the toolbars
        layoutInArea(grid, x, y, w, h, -1, HPos.CENTER, VPos.CENTER);
    }

}