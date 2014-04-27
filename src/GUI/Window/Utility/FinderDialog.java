package GUI.Window.Utility;

import GUI.Util.SearchToken;
import GUI.Widget.RichTextArea;
import GUI.Window.GuiWindow;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;

/**
 * Dialog window for searching within textArea objects.
 * Created by Matt Levine on 3/22/14.
 */
public final class FinderDialog extends UtilWindow {

    /* FIXME: the use of labels as spacing devices is probably a misuse */

    //    private BorderPane root;
    private final GuiWindow window;

    //0: case sensitive. 1: whole word. 2: regex. 3: single file
    private final String[] flags = {"","","",""};

    /** Builds a new FinderDialog **/
    public FinderDialog(GuiWindow window) {
        super("Search Dialog",400,400);
        buildOpacityButton();
        this.window = window;
    }

    /** Adds a widget to the finder **/
    @Override
    protected void addWidgets(){
        super.addWidgets();
        root.setStyle("-fx-background-color: #D6D6D6;");

        BorderPane searchPane = new BorderPane();

        final TextField searchField = new TextField();
        searchPane.setCenter(searchField);
        searchPane.setRight(new Label("         "));

        searchPane.setLeft(new Label("Text to search for: "));
        searchField.textProperty().addListener((observableValue, s, s2) ->
                window.find(new SearchToken(searchField.getText(),
                        SearchToken.SearchTokenType.valueOf("ALL" + flags[2] + flags[0] + flags[1] + flags[3]))));



        BorderPane replacePane = new BorderPane();
        final TextField replaceField = new TextField();
        replacePane.setCenter(replaceField);
        replacePane.setRight(new Label("        "));

        replacePane.setLeft(new Label("Replacement text: "));

        replacePane.setBottom(getButtons(searchField,replaceField));
        BorderPane searchAndReplacePane = new BorderPane();
        searchAndReplacePane.setTop(searchPane);
        searchAndReplacePane.setCenter(replacePane);
        root.setCenter(searchAndReplacePane);

    }

    /** Returns a Pane containing the options
     * @return a Pane containing options
     */
    private BorderPane getOptions(){
        BorderPane optionsPane = new BorderPane();

        //Case Sensitivity
        BorderPane layer1 = new BorderPane();
        RadioButton caseButton = new RadioButton();
        caseButton.setText("Case Sensitive");
        layer1.setTop(new Label("\n\n"));
        layer1.setCenter(caseButton);

        //Regex
        BorderPane layer2 = new BorderPane();
        RadioButton regexButton = new RadioButton();
        regexButton.setText("Regex");
        layer2.setTop(regexButton);
        layer1.setBottom(layer2);

        //Whole word
        BorderPane layer3 = new BorderPane();
        RadioButton wholeWord = new RadioButton();
        wholeWord.setText("Whole Word");
        layer3.setTop(new Label("\n\n"));
        layer3.setCenter(wholeWord);
        layer3.setRight(new Label("                                 "));

        //This file
        BorderPane layer4 = new BorderPane();
        RadioButton thisFile = new RadioButton();
        thisFile.setText("This File Only");
        layer4.setTop(thisFile);
        layer3.setBottom(thisFile);

        optionsPane.setLeft(layer1);
        optionsPane.setRight(layer3);
        optionsPane.setBottom(new Label("\n\n\n\n\n\n\n")); //this is sketchy
        configureOptions(caseButton,wholeWord,regexButton,thisFile);
        return optionsPane;
    }

    /** This configures the options; this is not to be used lackadaisically, it is designed
     * to be concise way of assigning listeners to the radio buttons. Careful consideration
     * of the code and the flags field should is advised before changing this method. The
     * input order of buttons must correspond to the order of the flags to avoid
     * unexpected behavior.
     * @param buttons The buttons to configure
     */
    private void configureOptions(final RadioButton... buttons){
        //this file on by default; ids must correspond to enum names
        buttons[0].setId("CS");
        buttons[1].setId("WW");
        buttons[2].setId("REG");
        buttons[3].setId("");
        buttons[3].setSelected(true);


        for (int i = 0; i < buttons.length; i++){
            final int j = i;
            buttons[i].selectedProperty().addListener((observableValue, aBoolean, aBoolean2) -> {
                flags[j] = aBoolean2 ? buttons[j].getId() : "";
                TextField searchField;
                try{
                    searchField = ((TextField)((BorderPane)root.getCenter()).getCenter());
                }catch (ClassCastException e){return;}
                if (j == 3 && !aBoolean2 ) buttons[3].setSelected(true);
                window.find(new SearchToken(searchField.getText(),
                        SearchToken.SearchTokenType.valueOf("ALL" + flags[2] + flags[0] + flags[1] + flags[3])));
            });
        }



    }

    /** Returns the main buttons **/
    private BorderPane getButtons(final TextField searchField,
                                  final TextField replaceField){
        final BorderPane buttons = new BorderPane();
        final BorderPane findButtons = new BorderPane();

        // -- Find Next
        findButtons.setLeft(getCoolButton("Find Next"));
        findButtons.setCenter(getCoolButton("Replace Next"));
        findButtons.setRight(getCoolButton("Replace All"));

        findButtons.getLeft().setOnMousePressed(mouseEvent ->
                window.find(new SearchToken(searchField.getText(),
                        SearchToken.SearchTokenType.valueOf("SINGLE" + flags[2] + flags[0] + flags[1] + flags[3]))));

        findButtons.getCenter().setOnMousePressed(mouseEvent ->
        window.replace(new SearchToken(searchField.getText(),
                                SearchToken.SearchTokenType.valueOf("SINGLE" + flags[2] + flags[0] + flags[1] + flags[3])),
                        replaceField.getText()

                ));

        findButtons.getRight().setOnMousePressed(mouseEvent ->
                window.replace(new SearchToken(searchField.getText(),
                                SearchToken.SearchTokenType.valueOf("ALL" + flags[2] + flags[0] + flags[1] + flags[3])),
                        replaceField.getText()

                ));

        //enter executes find next
        getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.ENTER,
                KeyCombination.SHORTCUT_ANY), () ->
                window.find(new SearchToken(searchField.getText(),
                        SearchToken.SearchTokenType.valueOf("SINGLE" + flags[2] + flags[0] + flags[1] + flags[3]))));
        getScene().getAccelerators().put(new KeyCodeCombination(KeyCode.ESCAPE,
                KeyCombination.SHORTCUT_ANY), this::hide);
        buttons.setBottom(findButtons);
        return buttons;
    }

    /** Returns a cool looking button
     * @return a cool looking button
     */
    private Button getCoolButton(String name){
        final Button button = new Button(name);
        button.setStyle("-fx-text-fill: white;\n" +
                "    -fx-font-family: \"Arial Narrow\";\n" +
                "    -fx-font-weight: bold;\n" +
                "    -fx-background-color: linear-gradient(#61a2b1, #2A5058);\n" +
                "    -fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.6) , 5, 0.0 , 0 , 1 );\n");

        button.setOnMouseEntered(mouseEvent ->
                button.setStyle("-fx-background-color: linear-gradient(#2A5058, #61a2b1);\n"));
        button.setOnMouseExited(mouseEvent ->
                button.setStyle("-fx-text-fill: white;\n" +
                        "    -fx-font-family: \"Arial Narrow\";\n" +
                        "    -fx-font-weight: bold;\n" +
                        "    -fx-background-color: linear-gradient(#61a2b1, #2A5058);\n" +
                        "    -fx-effect: dropshadow( three-pass-box , rgba(0,0,0,0.6) , 5, 0.0 , 0 , 1 );\n"));

        return button;
    }

    /** Builds an opacity button and adds it **/
    private void buildOpacityButton(){
        Button alphaIncr = new Button("+");
        alphaIncr.setOnMousePressed(mouseEvent -> {
            double newOpacitiy = getOpacity() * 1.1 > 1.0 ? 1.0 : getOpacity() * 1.1;
            setOpacity(newOpacitiy);
        });
        Button alphaDecr = new Button("-");
        alphaDecr.setOnMousePressed(mouseEvent ->
                setOpacity(getOpacity() / 1.1));

        BorderPane alphaPane = new BorderPane();
        alphaPane.setTop(getOptions());
        BorderPane leftAlphaPane = new BorderPane();
        leftAlphaPane.setLeft(alphaDecr);
        leftAlphaPane.setRight(alphaIncr);
        alphaPane.setLeft(leftAlphaPane);
        alphaPane.setBottom(new Label("\n\n\n")); //also sketchy

        formatButtonCool(alphaDecr, alphaIncr);
        root.setBottom(alphaPane);
    }

    /** Eliminates search results on close **/
    @Override protected void setCloseConditions(){
        setOnCloseRequest(windowEvent ->
                window.find(null));
    }

    /** Focuses on the input field **/
    public void focusInput(){
        ((BorderPane)root.getCenter()).getCenter().requestFocus();
    }

}
