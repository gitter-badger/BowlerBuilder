package com.neuronrobotics.bowlerbuilder.controller;

import static com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine.hasNetwork;

import com.google.common.base.Throwables;
import com.neuronrobotics.bowlerbuilder.GistUtilities;
import com.neuronrobotics.bowlerbuilder.LoggerUtilities;
import com.neuronrobotics.bowlerbuilder.controller.view.PreferencesController;
import com.neuronrobotics.bowlerbuilder.view.dialog.NewGistDialog;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.sdk.util.ThreadUtil;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Level;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.kohsuke.github.GHGist;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;

public class MainWindowController implements Initializable {

  @FXML
  private BorderPane root;
  @FXML
  private MenuItem logOut;
  @FXML
  private Menu myGists;
  @FXML
  private Menu myOrgs;
  @FXML
  private Menu myRepos;
  @FXML
  private Menu watchedRepos;
  @FXML
  private TabPane tabPane;
  @FXML
  private Tab homeTab;
  @FXML
  private SplitPane splitPane;
  @FXML
  private WebView homeWebView;
  @FXML
  private TextArea console;

  //Open file editors
  private final List<FileEditorController> fileEditors;
  private Map<String, Object> preferences;

  public MainWindowController() {
    fileEditors = new ArrayList<>();
    preferences = new HashMap<>();
    preferences.put("Font Size", 14); //TODO: Load previous font size preference
  }

  //Simple stream to append input characters to a text area
  private static class TextAreaPrintStream extends OutputStream {
    private final TextArea textArea;

    public TextAreaPrintStream(TextArea textArea) {
      this.textArea = textArea;
    }

    @Override
    public void write(int character) throws IOException {
      Platform.runLater(() -> textArea.appendText(String.valueOf((char) character)));
    }
  }

  @Override
  public void initialize(URL url, ResourceBundle resourceBundle) {
    //Add date to console
    console.setText(console.getText()
        + new SimpleDateFormat(
        "HH:mm:ss, MM dd, yyyy",
        new Locale("en", "US")).format(new Date())
        + "\n");

    //Redirect output to console
    PrintStream stream = null;
    try {
      stream = new PrintStream(new TextAreaPrintStream(console), true, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      LoggerUtilities.getLogger().log(Level.WARNING, "UTF-8 encoding unsupported.");
    }
    System.setOut(stream);
    System.setErr(stream);

    homeWebView
        .getEngine()
        .load("http://commonwealthrobotics.com/BowlerStudio/Welcome-To-BowlerStudio/");

    SplitPane.setResizableWithParent(console, false);
  }

  @FXML
  private void openNewCADFile(ActionEvent actionEvent) {
    try {
      NewGistDialog dialog = new NewGistDialog();
      dialog.getDialogPane().setId("newFileDialog");

      if (dialog.showAndWait().isPresent()) {
        List<String> nameAndDesc = dialog.getResult();
        boolean isPublic = dialog.getIsPublic();
        openFileInEditor(
            ScriptingEngine.fileFromGit( //TODO: Is this the correct way?
                GistUtilities.createNewGist(
                    nameAndDesc.get(0),
                    nameAndDesc.get(1),
                    isPublic),
                nameAndDesc.get(0)));
      }
    } catch (IOException e) {
      LoggerUtilities.getLogger().log(Level.WARNING,
          "Could not get full branch.\n" + Throwables.getStackTraceAsString(e));
    } catch (InvalidRemoteException e) {
      LoggerUtilities.getLogger().log(Level.WARNING,
          "Could not get file from git.\n" + Throwables.getStackTraceAsString(e));
    } catch (Exception e) {
      LoggerUtilities.getLogger().log(Level.WARNING,
          "Could not get files in git.\n" + Throwables.getStackTraceAsString(e));
    }
  }

  private void openFileInEditor(File file) {
    Tab tab = new Tab(file.getName());
    FXMLLoader loader = new FXMLLoader(getClass().getResource("../view/FileEditor.fxml"));
    try {
      tab.setContent(loader.load());

      final FileEditorController controller = loader.getController();
      fileEditors.add(controller);

      controller.setFontSize((int) preferences.get("Font Size"));
      controller.loadFile(file);

      tab.setOnCloseRequest(event -> fileEditors.remove(controller));
    } catch (IOException e) {
      LoggerUtilities.getLogger().log(Level.SEVERE,
          "Could not load FileEditor.fxml.\n" + Throwables.getStackTraceAsString(e));
    }
    tabPane.getTabs().add(tab);
    tabPane.getSelectionModel().select(tab);
  }

  @FXML
  private void exitProgram(ActionEvent actionEvent) {
    saveAndQuit();
  }

  @FXML
  private void logInToGitHub(ActionEvent actionEvent) {
    new Thread(() -> {
      Thread.currentThread().setName("GitHub Login Thread");

      ScriptingEngine.setLoginManager(s -> {
        VBox vBox = new VBox();
        TextField nameField = new TextField();
        PasswordField passField = new PasswordField();

        nameField.setPromptText("Username");
        passField.setPromptText("Password");

        vBox.setSpacing(5);
        vBox.getChildren().addAll(nameField, passField);

        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.getDialogPane().setContent(vBox);

        if (dialog.showAndWait().isPresent()) {
          return new String[]{dialog.getResult().getKey(), dialog.getResult().getValue()};
        } else {
          return new String[0];
        }
      });

      try {
        ScriptingEngine.runLogin();
        if (ScriptingEngine.isLoginSuccess() && hasNetwork()) {
          ScriptingEngine.setAutoupdate(true);
          logOut.setDisable(false);

          //Wait for GitHub to load in
          GitHub gitHub;
          while ((gitHub = ScriptingEngine.getGithub()) == null) {
            ThreadUtil.wait(20);
          }

          GHMyself myself = gitHub.getMyself();
          PagedIterable<GHGist> gists = myself.listGists();
          gists.forEach(gist -> {
            Menu gistMenu = new Menu(gist.getDescription()
                .substring(0, Math.min(15, gist.getDescription().length()))); //Cap length to 15

            MenuItem showWebGist = new MenuItem("Show Gist on Web");
            showWebGist.setOnAction(event -> {
              WebView webView = new WebView();
              webView.getEngine().load(gist.getHtmlUrl());
              Tab tab = new Tab(gist.getDescription(), webView);
              tabPane.getTabs().add(tab);
              tabPane.getSelectionModel().select(tab);
            });

            MenuItem addFileToGist = new MenuItem("Add File");
            addFileToGist.setOnAction(event -> Platform.runLater(() -> {
              try {
                openFileInEditor(ScriptingEngine.fileFromGit(gist.getGitPushUrl(),
                    ScriptingEngine.filesInGit(gist.getGitPushUrl(),
                        ScriptingEngine.getFullBranch(
                            gist.getGitPushUrl()),
                        null)
                        .get(0)));
              } catch (IOException e) {
                LoggerUtilities.getLogger().log(Level.WARNING,
                    "Could not get full branch.\n" + Throwables.getStackTraceAsString(e));
              } catch (InvalidRemoteException e) {
                LoggerUtilities.getLogger().log(Level.WARNING,
                    "Could not get file from git.\n" + Throwables.getStackTraceAsString(e));
              } catch (Exception e) {
                LoggerUtilities.getLogger().log(Level.WARNING,
                    "Could not get files in git.\n" + Throwables.getStackTraceAsString(e));
              }
            }));

            gistMenu.getItems().addAll(showWebGist, addFileToGist);
            myGists.getItems().add(gistMenu);
          });
        }
      } catch (IOException e) {
        LoggerUtilities.getLogger().log(Level.WARNING,
            "Could not launch GitHub as non-anonymous.\n" + Throwables.getStackTraceAsString(e));

        try {
          ScriptingEngine.setupAnyonmous();
        } catch (IOException e1) {
          LoggerUtilities.getLogger().log(Level.WARNING,
              "Could not launch GitHub anonymous.\n" + Throwables.getStackTraceAsString(e));
        }
      }
    }).start();
  }

  @FXML
  private void logOutFromGitHub(ActionEvent actionEvent) {
    try {
      ScriptingEngine.logout();
      logOut.setDisable(true);
      myGists.getItems().clear();
      myOrgs.getItems().clear();
      myRepos.getItems().clear();
    } catch (IOException e) {
      LoggerUtilities.getLogger().log(Level.WARNING,
          "Could not log out from GitHub.\n" + Throwables.getStackTraceAsString(e));
    }
  }

  @FXML
  private void onDeleteLocalCache(ActionEvent actionEvent) {
    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);

    alert.setTitle("Confirm Deletion");
    alert.setHeaderText("Delete All Local Files and Quit?");
    alert.setContentText("Deleting the cache will remove unsaved work and quit. Are you sure?");

    if (alert.showAndWait().isPresent() && alert.getResult() == ButtonType.OK) {
      new Thread(() -> {
        Thread.currentThread().setName("Delete Cache Thread");

        try {
          FileUtils.deleteDirectory(
              new File(
                  ScriptingEngine.getWorkspace().getAbsolutePath() + "/gistcache/"));
        } catch (IOException e) {
          LoggerUtilities.getLogger().log(Level.WARNING,
              "Unable to delete cache.\n" + Throwables.getStackTraceAsString(e));
        }

        Platform.runLater(this::quit);
      }).start();
    }
  }

  @FXML
  private void openPreferences(ActionEvent actionEvent) {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("../view/Preferences.fxml"));
    try {
      Node content = loader.load();
      Dialog dialog = new Dialog();
      dialog.getDialogPane().setContent(content);
      dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CLOSE);
      PreferencesController controller = loader.getController();
      controller.setPreferences(preferences);
      dialog.showAndWait();
      preferences = controller.getPreferences();
      if (controller.getPreferences().containsKey("Font Size")) {
        fileEditors.forEach(elem ->
            elem.setFontSize((Integer) controller.getPreferences().get("Font Size")));
      }
    } catch (IOException e) {
      LoggerUtilities.getLogger().log(Level.SEVERE,
          "Could not load Preferences.fxml.\n" + Throwables.getStackTraceAsString(e));
    }
  }

  /**
   * Save work and quit.
   */
  private void saveAndQuit() {
    //Save here
    quit();
  }

  /**
   * Quit the application.
   */
  private void quit() {
    root.getScene().getWindow().hide();
  }

}
