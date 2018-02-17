package com.neuronrobotics.bowlerbuilder.view.dialog;

import com.neuronrobotics.bowlerbuilder.FxUtil;
import com.neuronrobotics.bowlerbuilder.view.dialog.util.ValidatedTextField;
import java.util.Optional;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class GistFileSelectionDialog extends Dialog<String[]> {

  private final ValidatedTextField gistField;
  private final ComboBox<String> fileChooser;

  public GistFileSelectionDialog(String title) {
    super();

    gistField = new ValidatedTextField("Invalid Gist URL", url ->
        validateURL(url).isPresent());
    gistField.setId("gistField");

    fileChooser = new ComboBox<>();
    fileChooser.setId("gistFileChooser");
    fileChooser.setDisable(true);

    gistField.invalidProperty().bind(fileChooser.disableProperty());
    gistField.invalidProperty().addListener((observable, oldValue, newValue) -> {
      if (!newValue) {
        System.out.println("event");
      }
    });

    setTitle(title);

    GridPane pane = new GridPane();
    pane.setId("root");
    pane.setAlignment(Pos.CENTER);
    pane.setHgap(5);
    pane.setVgap(5);

    pane.add(new Label("Gist URL"), 0, 0);
    pane.add(gistField, 1, 0);
    pane.add(new Label("File name"), 0, 1);
    pane.add(fileChooser, 1, 1);

    getDialogPane().setContent(pane);
    getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    FxUtil.runFX(gistField::requestFocus);

    Button addButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
    addButton.disableProperty().bind(gistField.invalidProperty());
    addButton.setDefaultButton(true);

    setResultConverter(buttonType -> {
      if (buttonType.equals(ButtonType.OK)) {
        return new String[]{gistField.getText(),
            fileChooser.getSelectionModel().getSelectedItem()};
      }

      return null;
    });
  }

  /**
   * Will accept http:// or https:// with .git or .git/.
   *
   * @param url gist URL
   * @return optional containing a valid gist URL, empty otherwise
   */
  private Optional<String> validateURL(String url) {
    //Any git URL is ((git|ssh|http(s)?)|(git@[\w\.]+))(:(//)?)([\w\.@\:/\-~]+)(\.git)(/)?
    if (url.matches("(http(s)?)(:(//)?)([\\w\\.@\\:/\\-~]+)(\\.git)(/)?")) {
      return Optional.of(url);
    }

    return Optional.empty();
  }

}