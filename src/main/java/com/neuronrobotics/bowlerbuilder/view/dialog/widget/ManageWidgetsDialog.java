package com.neuronrobotics.bowlerbuilder.view.dialog.widget;

import com.neuronrobotics.bowlerbuilder.controller.widget.Widget;
import java.util.List;
import java.util.Optional;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ManageWidgetsDialog extends Dialog<List<Widget>> {

  private final ListView<Widget> widgetListView;

  public ManageWidgetsDialog(ObservableList<Widget> widgets) {
    super();

    widgetListView = new ListView<>(widgets);
    widgetListView.setId("widgetList");

    Button addWidgetButton = new Button("Add Widget");
    addWidgetButton.setId("addWidget");
    addWidgetButton.setOnAction(event -> {
      AddWidgetDialog dialog = new AddWidgetDialog();
      final Optional<Boolean> result = dialog.showAndWait();
      if (result.isPresent() && result.get()) {
        widgets.add(new Widget(dialog.getSource(), dialog.getDisplayName()));
      }
    });

    Button removeWidgetButton = new Button("Remove Widget");
    removeWidgetButton.setId("removeWidget");
    removeWidgetButton.setOnAction(event ->
        widgetListView.getSelectionModel().getSelectedItems()
            .forEach(widgetListView.getItems()::remove));

    HBox controlBox = new HBox(5, addWidgetButton, removeWidgetButton);

    VBox view = new VBox(5, controlBox, widgetListView);

    setTitle("Manage Widgets");
    getDialogPane().setContent(view);
    getDialogPane().getButtonTypes().add(ButtonType.OK);
    getDialogPane().setId("manageWidgetsDialogPane");

    setResultConverter(buttonType -> widgetListView.getItems());
  }

  public void setWidgets(ObservableList<Widget> widgets) {
    widgetListView.setItems(widgets);
  }

}
