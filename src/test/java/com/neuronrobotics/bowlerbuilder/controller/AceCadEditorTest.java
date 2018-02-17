package com.neuronrobotics.bowlerbuilder.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.inject.Guice;
import com.neuronrobotics.bowlerbuilder.AutoClosingApplicationTest;
import com.neuronrobotics.bowlerbuilder.BowlerBuilder;
import com.neuronrobotics.bowlerbuilder.FxHelper;
import com.neuronrobotics.bowlerbuilder.FxUtil;
import com.neuronrobotics.bowlerbuilder.controller.module.AceCadEditorControllerModule;
import com.neuronrobotics.bowlerbuilder.controller.scripting.scripteditor.ace.AceEditorView;
import eu.mihosoft.vrl.v3d.CSG;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

public class AceCadEditorTest extends AutoClosingApplicationTest {

  private AceScriptEditorController controller;

  @Override
  public void start(Stage stage) throws Exception {
    FXMLLoader loader = new FXMLLoader(
        getClass().getResource("../view/AceScriptEditor.fxml"),
        null,
        null,
        Guice.createInjector(
            new AceCadEditorControllerModule(
                BowlerBuilder.getInjector().getInstance(AceEditorView.class)))::getInstance);
    SplitPane mainWindow = loader.load();
    controller = loader.getController();
    stage.setScene(new Scene(mainWindow));
    stage.show();
  }

  @Test
  void runEmptyFileTest() {
    FxHelper.runAndWait(() -> controller.runEditorContent());

    assertNull(controller.getScriptRunner().resultProperty().get());
  }

  @Test
  void basicRunButtonTest() {
    FxHelper.runAndWait(() -> controller.insertAtCursor("CSG foo=new Cube(10,10,10).toCSG()"));
    FxHelper.runAndWait(() -> controller.runEditorContent());

    assertTrue(controller.getScriptRunner().resultProperty().get() instanceof CSG);
  }

  @Test
  void runCubeTest() {
    FxHelper.runAndWait(() -> controller.insertAtCursor("CSG foo=new Cube(10,10,10).toCSG()"));
    controller.runEditorContent();

    assertNotNull(controller.getScriptRunner().resultProperty().get());
  }

  @Test
  void runStringScriptTest() {
    controller.runStringScript("CSG foo = new Sphere(10).toCSG();", new ArrayList<>(),
        "BowlerGroovy");

    assertTrue(controller.getScriptRunner().resultProperty().get() instanceof CSG);
  }

  @Test
  void getTextTest() throws ExecutionException, InterruptedException {
    FxHelper.runAndWait(() -> controller.getScriptEditor().insertAtCursor("foo\nbar"));

    assertEquals("foo\nbar", FxUtil.returnFX(() -> controller.getScriptEditor().getText()));
  }

}
