package com.neuronrobotics.bowlerbuilder.controller.scripteditor;

import static org.junit.Assert.assertEquals;

import com.neuronrobotics.bowlerbuilder.controller.scripteditor.ace.AceEditor;
import org.junit.jupiter.api.Test;

public class AceInterfaceTest {

  private final MockAdapter mockAdapter = new MockAdapter();
  private final AceEditor aceEditor = new AceEditor(mockAdapter);

  @Test
  public void insertTextTest() {
    aceEditor.insertAtCursor("test");
    assertEquals("editor.insert(\"test\");", mockAdapter.lastExecutedScript);
  }

  @Test
  public void insertTextTest2() {
    aceEditor.insertAtCursor("CSG foo = new Cube(1,1,1).toCSG();");
    assertEquals("editor.insert(\"CSG foo = new Cube(1,1,1).toCSG();\");",
        mockAdapter.lastExecutedScript);
  }

  @Test
  public void insertTextTest3() {
    aceEditor.insertAtCursor("\n\t");
    assertEquals("editor.insert(\"\\n\t\");",
        mockAdapter.lastExecutedScript);
  }

  @Test
  public void insertTextTest4() {
    aceEditor.insertAtCursor("\\'\"");
    assertEquals("editor.insert(\"\\\\'\\\"\");",
        mockAdapter.lastExecutedScript);
  }

  @Test
  public void fontSizeTest() {
    aceEditor.setFontSize(1);
    assertEquals("document.getElementById('editor').style.fontSize='1px';",
        mockAdapter.lastExecutedScript);
  }

  @Test
  public void gotoLineTest() {
    aceEditor.gotoLine(1);
    assertEquals("editor.gotoLine(1);",
        mockAdapter.lastExecutedScript);
  }

}
