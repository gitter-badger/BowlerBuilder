package com.neuronrobotics.bowlerbuilder.view.dialog;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;

public class PublishDialog extends Dialog<String> {

  private final TextArea commitMessage;

  public PublishDialog() {
    super();

    commitMessage = new TextArea();
    commitMessage.setId("commitMessageTextArea");
    commitMessage.setPrefHeight(100);
    commitMessage.setPrefWidth(250);

    GridPane pane = new GridPane();
    pane.setId("publishDialogRoot");
    pane.setAlignment(Pos.CENTER);
    pane.setHgap(5);
    pane.setVgap(5);

    pane.add(new Label("Commit Message"), 0, 0);
    pane.add(commitMessage, 1, 0);

    setTitle("Enter a Commit Message");
    getDialogPane().setContent(pane);
    getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

    Platform.runLater(commitMessage::requestFocus);

    setResultConverter(buttonType -> {
      if (buttonType == ButtonType.OK) {
        return getCommitMessage();
      }

      return null;
    });
  }

  public String getCommitMessage() {
    return commitMessage.getText();
  }

}
