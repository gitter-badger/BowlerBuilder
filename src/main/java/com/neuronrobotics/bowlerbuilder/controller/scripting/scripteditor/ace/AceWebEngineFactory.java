package com.neuronrobotics.bowlerbuilder.controller.scripting.scripteditor.ace;

import javafx.scene.web.WebEngine;

public class AceWebEngineFactory {

  public AceWebEngine create(WebEngine webEngine) {
    return new AceWebEngine(webEngine);
  }

}
