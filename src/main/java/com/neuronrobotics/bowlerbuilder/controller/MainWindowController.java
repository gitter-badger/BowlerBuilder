package com.neuronrobotics.bowlerbuilder.controller;

import com.neuronrobotics.bowlerbuilder.LoggerUtilities;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

public class MainWindowController implements Initializable {

  @FXML
  private BorderPane root;
  @FXML
  private TabPane tabPane;

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    //Nothing to initialize yet
  }

  @FXML
  private void openNewCADFile(ActionEvent actionEvent) {
    Tab tab = new Tab("New file");
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("../view/FileEditor.fxml"));
      Node content = loader.load();
      tab.setContent(content);
    } catch (IOException e) {
      LoggerUtilities.getLogger().log(Level.SEVERE, "Couldn't load FileEditor.fxml");
    }
    tabPane.getTabs().add(tab);
    tabPane.getSelectionModel().select(tab);
  }

  @FXML
  private void exitProgram(ActionEvent actionEvent) {
    root.getScene().getWindow().hide();
  }

}
