package com.neuronrobotics.bowlerbuilder.controller; //NOPMD

import static com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine.hasNetwork;

import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.neuronrobotics.bowlerbuilder.FxUtil;
import com.neuronrobotics.bowlerbuilder.LoggerUtilities;
import com.neuronrobotics.bowlerbuilder.controller.widget.Widget;
import com.neuronrobotics.bowlerbuilder.model.preferences.PreferencesService;
import com.neuronrobotics.bowlerbuilder.model.preferences.PreferencesServiceFactory;
import com.neuronrobotics.bowlerbuilder.view.dialog.AddFileToGistDialog;
import com.neuronrobotics.bowlerbuilder.view.dialog.HelpDialog;
import com.neuronrobotics.bowlerbuilder.view.dialog.LoginDialog;
import com.neuronrobotics.bowlerbuilder.view.dialog.PreferencesDialog;
import com.neuronrobotics.bowlerbuilder.view.dialog.widget.ManageWidgetsDialog;
import com.neuronrobotics.bowlerbuilder.view.tab.AceCadEditorTab;
import com.neuronrobotics.bowlerstudio.assets.AssetFactory;
import com.neuronrobotics.bowlerstudio.scripting.ScriptingEngine;
import com.neuronrobotics.bowlerstudio.vitamins.Vitamins;
import com.neuronrobotics.sdk.util.ThreadUtil;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.kohsuke.github.GHGist;
import org.kohsuke.github.GHGistFile;
import org.kohsuke.github.GHMyself;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHPersonSet;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;

@Singleton
public class MainWindowController {

  private static final Logger logger =
      LoggerUtilities.getLogger(MainWindowController.class.getSimpleName());
  private final PreferencesServiceFactory preferencesServiceFactory;
  private final PreferencesService preferencesService;
  //Open file editors
  private final List<AceCadEditorController> fileEditors;

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
  private Menu cadVitamins;
  @FXML
  private Menu installedWidgets;
  @FXML
  private TabPane tabPane;
  @FXML
  private Tab homeTab;
  @FXML
  private WebBrowserController webBrowserController;
  @FXML
  private TextArea console;

  @Inject
  protected MainWindowController(PreferencesServiceFactory preferencesServiceFactory) {
    this.preferencesServiceFactory = preferencesServiceFactory;
    this.fileEditors = new ArrayList<>();

    preferencesService = preferencesServiceFactory.create("MainWindowController");
    preferencesService.load();
  }

  @FXML
  protected void initialize() {
    //Add date to console
    console.setText(
        console.getText()
            + new SimpleDateFormat(
            "HH:mm:ss, MM dd, yyyy",
            new Locale("en", "US")).format(new Date())
            + "\n");

    //Redirect output to console
    PrintStream stream = null;
    try {
      stream = new PrintStream(new TextAreaPrintStream(console), true, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      logger.log(Level.SEVERE, "UTF-8 encoding not supported.");
    }

    System.setOut(stream);
    System.setErr(stream);

    loadPage("http://commonwealthrobotics.com/BowlerStudio/Welcome-To-BowlerStudio/");

    SplitPane.setResizableWithParent(console, false);

    try {
      ScriptingEngine.runLogin();
      if (ScriptingEngine.isLoginSuccess() && hasNetwork()) {
        setupMenusOnLogin();

        try {
          ScriptingEngine.filesInGit(AssetFactory.getGitSource(), "0.25.1", null);
        } catch (Exception e) {
          logger.log(Level.WARNING,
              "Could not preload assets.\n" + Throwables.getStackTraceAsString(e));
        }
      }
    } catch (IOException e) {
      logger.log(Level.WARNING,
          "Could not automatically log in.\n");
      logOut.setDisable(true); //Can't log out when not logged in
    }

    reloadWidgets(preferencesService.get("Widgets", new ArrayList<>()));
  }

  @FXML
  private void openPreferences(ActionEvent actionEvent) {
    new PreferencesDialog(preferencesServiceFactory.getAllPreferencesServices()).showAndWait();
  }

  @FXML
  private void onLogOutFromGitHub(ActionEvent actionEvent) {
    try {
      ScriptingEngine.logout();
      logOut.setDisable(true);
      myGists.getItems().clear();
      myOrgs.getItems().clear();
      myRepos.getItems().clear();
    } catch (IOException e) {
      logger.log(Level.SEVERE,
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
          logger.log(Level.SEVERE,
              "Unable to delete cache.\n" + Throwables.getStackTraceAsString(e));
        }

        FxUtil.runFX(this::quit);
      }).start();
    }
  }

  @FXML
  private void onExitProgram(ActionEvent actionEvent) {
    saveAndQuit();
  }

  @FXML
  private void onLogInToGitHub(ActionEvent actionEvent) {
    tryLogin();
  }

  @FXML
  private void onReloadMenus(ActionEvent actionEvent) {
    reloadGitMenus();
  }

  @FXML
  private void onOpenScratchpad(ActionEvent actionEvent) {
    try {
      AceCadEditorTab tab = new AceCadEditorTab("Scratchpad");
      AceCadEditorController controller = tab.getController();

      controller.initScratchpad(tab, this::reloadGitMenus);
      fileEditors.add(controller);

      tab.setOnCloseRequest(event -> fileEditors.remove(controller));

      tabPane.getTabs().add(tab);
      tabPane.getSelectionModel().select(tab);
    } catch (IOException e) {
      logger.log(Level.SEVERE,
          "Could not load AceCadEditor.fxml.\n" + Throwables.getStackTraceAsString(e));
    }
  }

  @FXML
  private void onReloadVitamins(ActionEvent actionEvent) {
    reloadCadMenus();
  }

  @FXML
  private void onManageWidgets(ActionEvent actionEvent) {
    ManageWidgetsDialog dialog = new ManageWidgetsDialog(FXCollections.observableArrayList(
        preferencesService.get("Widgets", new ArrayList<>())));
    dialog.showAndWait().ifPresent(widgets -> {
      preferencesService.set("Widgets", new ArrayList<>(widgets));
      reloadWidgets(widgets);
    });
  }

  @FXML
  private void openEditorHelp(ActionEvent actionEvent) {
    new HelpDialog().showAndWait();
  }

  /**
   * Load a page into the home WebView.
   *
   * @param url URL to load
   */
  public void loadPage(String url) {
    FxUtil.runFX(() -> webBrowserController.loadPage(url));
  }

  /**
   * Load a page into a new Tab.
   *
   * @param tabName name for new tab
   * @param url URL to load
   */
  public void loadPageIntoNewTab(String tabName, String url) {
    FxUtil.runFX(() -> {
      FXMLLoader loader = new FXMLLoader(MainWindowController.class.getResource(
          "/com/neuronrobotics/bowlerbuilder/view/WebBrowser.fxml"));

      try {
        Tab tab = new Tab(tabName, loader.load());
        WebBrowserController controller = loader.getController();
        controller.loadPage(url);
        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
      } catch (IOException e) {
        logger.log(Level.SEVERE,
            "Could not load WebBrowser.\n" + Throwables.getStackTraceAsString(e));
      }
    });
  }

  /**
   * Open a gist file in the file editor.
   *
   * @param gist Gist containing file
   * @param gistFile File
   */
  public void openGistFileInEditor(GHGist gist, GHGistFile gistFile) {
    FxUtil.runFX(() -> {
      try {
        AceCadEditorTab tab = new AceCadEditorTab(gistFile.getFileName());
        AceCadEditorController controller = tab.getController();

        controller.loadGist(gist, gistFile);
        fileEditors.add(tab.getController());

        tab.setOnCloseRequest(event -> fileEditors.remove(controller));

        tabPane.getTabs().add(tab);
        tabPane.getSelectionModel().select(tab);
      } catch (IOException e) {
        logger.log(Level.SEVERE,
            "Could not load AceCadEditor.fxml.\n" + Throwables.getStackTraceAsString(e));
      }
    });
  }

  private void tryLogin() {
    ScriptingEngine.setLoginManager(s -> {
      LoginDialog dialog = new LoginDialog();

      final Optional<Boolean> result = dialog.showAndWait();
      if (result.isPresent() && result.get()) {
        return new String[]{dialog.getName(), dialog.getPassword()};
      } else {
        return new String[0];
      }
    });

    try {
      ScriptingEngine.waitForLogin();
      if (ScriptingEngine.isLoginSuccess() && hasNetwork()) {
        setupMenusOnLogin();
      }
    } catch (IOException e) {
      logger.log(Level.SEVERE,
          "Could not launch GitHub as non-anonymous.\n" + Throwables.getStackTraceAsString(e));
      try {
        ScriptingEngine.setupAnyonmous();
      } catch (IOException e1) {
        logger.log(Level.SEVERE,
            "Could not launch GitHub anonymous.\n" + Throwables.getStackTraceAsString(e));
      }
    } catch (GitAPIException e) {
      logger.log(Level.SEVERE,
          "Could not log in.\n" + Throwables.getStackTraceAsString(e));
    }
  }

  /**
   * Setup the menus for the main menu bar.
   */
  private void setupMenusOnLogin() {
    try {
      ScriptingEngine.setAutoupdate(true);
    } catch (IOException e) {
      logger.log(Level.SEVERE,
          "Could not set auto update.\n" + Throwables.getStackTraceAsString(e));
    }

    logOut.setDisable(false);

    reloadGitMenus();
    reloadCadMenus();
  }

  /**
   * Reload the GitHub-related menus.
   */
  public void reloadGitMenus() {
    FxUtil.runFX(() -> {
      //Wait for GitHub to load in
      GitHub gitHub;

      while ((gitHub = ScriptingEngine.getGithub()) == null) {
        ThreadUtil.wait(20);
      }

      myGists.getItems().clear();
      myOrgs.getItems().clear();
      myRepos.getItems().clear();

      GHMyself myself;
      try {
        myself = gitHub.getMyself();

        LoggerUtilities.newLoggingThread(logger, () -> {
          try {
            loadGistsIntoMenus(myGists, myself.listGists());
          } catch (IOException e) {
            logger.log(Level.SEVERE,
                "Unable to list gists.\n" + Throwables.getStackTraceAsString(e));
          }
        }).start();

        LoggerUtilities.newLoggingThread(logger, () -> {
          try {
            loadOrgsIntoMenus(myOrgs, myself.getAllOrganizations());
          } catch (IOException e) {
            logger.log(Level.SEVERE,
                "Unable to get organizations.\n" + Throwables.getStackTraceAsString(e));
          }
        }).start();

        LoggerUtilities.newLoggingThread(logger, () ->
            loadReposIntoMenus(myRepos, myself.listRepositories())).start();
      } catch (IOException e) {
        logger.log(Level.SEVERE,
            "Could not get GitHub.\n" + Throwables.getStackTraceAsString(e));
      }
    });
  }

  /**
   * Reload the CAD menus.
   */
  public void reloadCadMenus() {
    FxUtil.runFX(() -> {
      cadVitamins.getItems().clear();

      LoggerUtilities.newLoggingThread(logger, () ->
          Vitamins.listVitaminTypes().stream().sorted().forEach(vitamin -> {

            Menu vitaminMenu = new Menu(vitamin);

            Vitamins.listVitaminSizes(vitamin).stream().sorted().forEach(size -> {
              MenuItem sizeMenu = new MenuItem(size);

              sizeMenu.setOnAction(event1 -> {
                Tab selection = tabPane.getSelectionModel().getSelectedItem();
                if (selection instanceof AceCadEditorTab) {
                  AceCadEditorTab editorTab = (AceCadEditorTab) selection;
                  String insertion =
                      "CSG foo = Vitamins.get(\"" + vitamin + "\", \"" + size + "\");";
                  editorTab.getController().insertAtCursor(insertion);
                }
              });

              vitaminMenu.getItems().add(sizeMenu);
            });

            cadVitamins.getItems().add(vitaminMenu);
          })).start();
    });
  }

  /**
   * Load gists into menus for the main menu bar.
   *
   * @param menu menu to put submenus into
   * @param gists list of gists
   */
  private void loadGistsIntoMenus(Menu menu, PagedIterable<GHGist> gists) {
    gists.forEach(gist -> {
      MenuItem showWebGist = new MenuItem("Show Gist on Web");
      showWebGist.setOnAction(event ->
          loadPageIntoNewTab(gist.getDescription(), gist.getHtmlUrl()));

      MenuItem addFileToGist = new MenuItem("Add File");
      addFileToGist.setOnAction(event -> FxUtil.runFX(() -> {
        AddFileToGistDialog dialog = new AddFileToGistDialog();
        dialog.showAndWait().ifPresent(name -> {
          try {
            ScriptingEngine.pushCodeToGit(
                gist.getGitPushUrl(),
                ScriptingEngine.getFullBranch(gist.getGitPushUrl()),
                name,
                "//Your code here",
                "New file");
            reloadGitMenus();
            ScriptingEngine.getGithub().getMyself().listGists().asList()
                .stream()
                .filter(item -> item.equals(gist))
                .findFirst()
                .ifPresent(newGist -> openGistFileInEditor(newGist, newGist.getFile(name)));
          } catch (Exception e) {
            logger.log(Level.SEVERE,
                "Could not add file to gist.\n" + Throwables.getStackTraceAsString(e));
          }
        });
      }));

      MenuItem addFileFromDisk = new MenuItem("Add File from Disk");
      addFileFromDisk.setOnAction(event -> FxUtil.runFX(() -> {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select File to Add");
        File selection = fileChooser.showOpenDialog(root.getScene().getWindow());
        if (selection != null && selection.isFile()) {
          try {
            ScriptingEngine.pushCodeToGit(
                gist.getGitPushUrl(),
                ScriptingEngine.getFullBranch(gist.getGitPushUrl()),
                selection.getName(),
                Files.readLines(selection, Charsets.UTF_8).stream()
                    .collect(Collectors.joining("\n")),
                "Add file: " + selection.getName());
            reloadGitMenus();
          } catch (Exception e) {
            logger.log(Level.SEVERE,
                "Could not add file from disk to gist.\n" + Throwables.getStackTraceAsString(e));
          }
        }
      }));

      String gistMenuText = gist.getDescription();
      if (gistMenuText == null || gistMenuText.length() == 0) {
        Set<String> filenames = gist.getFiles().keySet();
        if (filenames.size() >= 1) {
          gistMenuText = filenames.iterator().next();
        } else {
          gistMenuText = "";
        }
      }

      //Cap length
      gistMenuText = gistMenuText.substring(0, Math.min(25, gistMenuText.length()));

      Menu gistMenu = new Menu(gistMenuText);
      gistMenu.getItems().addAll(showWebGist, addFileToGist, addFileFromDisk);

      gist.getFiles().forEach((name, gistFile) -> {
        MenuItem gistFileItem = new MenuItem(name);
        gistFileItem.setOnAction(event -> openGistFileInEditor(gist, gistFile));
        gistMenu.getItems().add(gistFileItem);
      });

      menu.getItems().add(gistMenu);
    });
  }

  /**
   * Load organizations into menus for the main menu bar.
   *
   * @param menu menu to put submenus into
   * @param orgs organizations
   */
  private void loadOrgsIntoMenus(Menu menu, GHPersonSet<GHOrganization> orgs) {
    Function<GHOrganization, String> getName = org -> {
      try {
        String name = org.getName();
        if (name == null || name.length() == 0) {
          name = org.getLogin();
        }
        return name;
      } catch (IOException e) {
        logger.log(Level.SEVERE,
            "Error while sanitizing organization name.\n" + Throwables.getStackTraceAsString(e));
      }

      return "";
    };

    orgs.stream().sorted(Comparator.comparing(getName)).forEach(org -> {
      try {
        Menu orgMenu = new Menu(getName.apply(org));
        org.getRepositories().forEach((key, value) -> {
          MenuItem repoMenu = new MenuItem(key);
          repoMenu.setOnAction(event -> {
                loadPageIntoNewTab(
                    value.getDescription()
                        .substring(0, Math.min(15, value.getDescription().length())),
                    value.gitHttpTransportUrl());
                event.consume();
              }
          );
          orgMenu.getItems().add(repoMenu);
        });

        orgMenu.setOnAction(event -> {
          try {
            loadPageIntoNewTab(org.getName(), org.getHtmlUrl());
          } catch (IOException e) {
            logger.log(Level.SEVERE,
                "Could not get organization name when loading new tab.\n"
                    + Throwables.getStackTraceAsString(e));
          }
        });

        menu.getItems().add(orgMenu);
      } catch (IOException e) {
        logger.log(Level.SEVERE,
            "Unable to get name of organization.\n" + Throwables.getStackTraceAsString(e));
      }
    });
  }

  /**
   * Load repositories into menus for the main menu bar.
   *
   * @param menu menu to put submenus into
   * @param repos repositories
   */
  private void loadReposIntoMenus(Menu menu, PagedIterable<GHRepository> repos) {
    repos.asList().stream().sorted(Comparator.comparing(GHRepository::getName)).forEach(repo -> {
      MenuItem menuItem = new MenuItem(repo.getName());
      menuItem.setOnAction(event ->
          loadPageIntoNewTab(
              repo.getName().substring(0, Math.min(15, repo.getName().length())),
              repo.gitHttpTransportUrl()));
      menu.getItems().add(menuItem);
    });
  }

  private void reloadWidgets(List<Widget> widgets) {
    installedWidgets.getItems().clear();
    installedWidgets.getItems().addAll(widgets.stream().map(widget -> {
      MenuItem item = new MenuItem(widget.getDisplayName());
      item.setOnAction(event -> {
        try {
          widget.run();
        } catch (Exception e) {
          logger.log(Level.SEVERE,
              "Unable to run widget " + widget.getGitSource() + "\n"
                  + Throwables.getStackTraceAsString(e));
        }
      });
      return item;
    }).collect(Collectors.toList()));
  }

  /**
   * Save work and quit.
   */
  public void saveAndQuit() {
    preferencesServiceFactory.saveAllCached();
    quit();
  }

  /**
   * Quit the application.
   */
  private void quit() {
    root.getScene().getWindow().hide();
    Platform.exit();
  }

  /**
   * Add a tab to the tab pane.
   *
   * @param tab tab to add
   */
  public void addTab(Tab tab) {
    FxUtil.runFX(() -> tabPane.getTabs().add(tab));
  }

  /**
   * Get the currently selected tab.
   *
   * @return current tab
   */
  public Optional<Tab> getSelectedTab() {
    try {
      return Optional.of(FxUtil.returnFX(() -> tabPane.getSelectionModel().getSelectedItem()));
    } catch (ExecutionException | InterruptedException e) {
      logger.log(Level.SEVERE,
          "Could not get selected tab.\n" + Throwables.getStackTraceAsString(e));
    }

    return Optional.empty();
  }

  //Simple stream to append input characters to a text area
  private static class TextAreaPrintStream extends OutputStream {

    private final TextArea textArea;

    public TextAreaPrintStream(TextArea textArea) {
      this.textArea = textArea;
    }

    @Override
    public void write(int character) {
      FxUtil.runFX(() -> textArea.appendText(String.valueOf((char) character)));
    }
  }

}
