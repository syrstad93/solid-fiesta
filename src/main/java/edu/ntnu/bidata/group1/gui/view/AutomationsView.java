package edu.ntnu.bidata.group1.gui.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.LongConsumer;

import edu.ntnu.bidata.group1.device.actuator.CommandSpec;
import edu.ntnu.bidata.group1.device.actuator.ParameterType;
import edu.ntnu.bidata.group1.device.controller.ControlledDevice;
import edu.ntnu.bidata.group1.gui.model.HomeModel;
import edu.ntnu.bidata.group1.utility.Automation;
import edu.ntnu.bidata.group1.utility.Condition;
import edu.ntnu.bidata.group1.utility.Permission;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * The Automations tab: the rules the server follows, such as "when the temperature goes below
 * 20, turn the heater on". Users and owners can add and remove them; guests can only look.
 */
public class AutomationsView extends BorderPane {

  private final Label notice = new Label();
  private final ListView<Choice> list = new ListView<>();
  private final Button removeButton = new Button("Remove selected");

  private final ComboBox<Choice> sensor = new ComboBox<>();
  private final ComboBox<String> reading = new ComboBox<>();
  private final ComboBox<Condition> condition = new ComboBox<>();
  private final TextField threshold = new TextField();
  private final ComboBox<Choice> device = new ComboBox<>();
  private final ComboBox<String> command = new ComboBox<>();
  private final TextField argument = new TextField();
  private final Label argumentHint = new Label();
  private final Button addButton = new Button("Add automation");
  private final VBox form;

  private HomeModel model;

  /** Creates the tab. */
  public AutomationsView() {
    notice.getStyleClass().add("notice");
    notice.setWrapText(true);

    list.setPlaceholder(new Label("No automations yet"));
    VBox left = new VBox(10, new Label("Automations"), list, removeButton);
    VBox.setVgrow(list, Priority.ALWAYS);
    left.getStyleClass().add("panel");
    HBox.setHgrow(left, Priority.ALWAYS);

    sensor.setPromptText("Device to watch");
    reading.setPromptText("Reading");
    reading.setEditable(true);
    condition.getItems().setAll(Condition.values());
    condition.setValue(Condition.ABOVE);
    threshold.setPromptText("e.g. 22.5");
    device.setPromptText("Device to control");
    command.setPromptText("Command");
    argument.setPromptText("Argument");
    argumentHint.getStyleClass().add("muted");
    for (var box : List.of(sensor, reading, condition, device, command)) {
      box.setMaxWidth(Double.MAX_VALUE);
    }

    sensor.setOnAction(event -> updateReadings());
    device.setOnAction(event -> updateCommands());
    command.setOnAction(event -> updateArgumentHint());

    GridPane grid = new GridPane();
    grid.setHgap(10);
    grid.setVgap(10);
    int row = 0;
    grid.addRow(row++, new Label("When device"), sensor);
    grid.addRow(row++, new Label("reports"), reading);
    grid.addRow(row++, new Label("that goes"), condition);
    grid.addRow(row++, new Label("threshold"), threshold);
    grid.addRow(row++, new Label("send to"), device);
    grid.addRow(row++, new Label("command"), command);
    grid.addRow(row++, new Label("argument"), argument);
    grid.add(argumentHint, 1, row);
    GridPane.setHgrow(sensor, Priority.ALWAYS);

    Label title = new Label("New automation");
    title.getStyleClass().add("section-title");
    form = new VBox(12, title, grid, addButton);
    form.getStyleClass().add("panel");
    form.setPrefWidth(380);

    HBox content = new HBox(16, left, form);
    content.setPadding(new Insets(16));
    setTop(notice);
    BorderPane.setMargin(notice, new Insets(16, 16, 0, 16));
    setCenter(content);
  }

  /**
   * Sets what happens when the user adds an automation.
   *
   * @param listener receives what the user filled in
   */
  public void setOnAddAutomation(Consumer<AutomationRequest> listener) {
    addButton.setOnAction(event -> listener.accept(new AutomationRequest(
        sensor.getValue(), reading.getEditor().getText(), condition.getValue(),
        threshold.getText(), device.getValue(), command.getValue(), argument.getText())));
  }

  /**
   * Sets what happens when the user removes the selected automation.
   *
   * @param listener receives the automation's id
   */
  public void setOnRemoveAutomation(LongConsumer listener) {
    removeButton.setOnAction(event -> {
      Choice selected = list.getSelectionModel().getSelectedItem();
      if (selected != null) {
        listener.accept(selected.id());
      }
    });
  }

  /**
   * Shows the model's automations, and the devices to choose from.
   *
   * @param model the model
   */
  public void render(HomeModel model) {
    this.model = model;
    boolean canManage = model.can(Permission.MANAGE_AUTOMATIONS);
    notice.setText("Your role (" + Texts.title(model.getRole())
        + ") can see automations, but not change them. Users and owners can.");
    notice.setVisible(!canManage);
    notice.setManaged(!canManage);
    form.setDisable(!canManage);
    removeButton.setDisable(!canManage);

    List<Choice> automations = new ArrayList<>();
    for (Automation automation : model.getAutomations()) {
      automations.add(new Choice(automation.getId(), describe(automation)));
    }
    Choice.setItems(list, automations);

    List<Choice> devices = new ArrayList<>();
    for (ControlledDevice d : model.getDevices()) {
      devices.add(new Choice(d.getId(), model.deviceLabel(d.getId()) + "  #" + d.getId()));
    }
    Choice.setItems(sensor, devices);
    Choice.setItems(device, devices);
    updateReadings();
    updateCommands();
  }

  private String describe(Automation automation) {
    String threshold = String.format(Locale.ROOT, "%.1f", automation.getThreshold());
    return "When " + automation.getReading() + " on "
        + model.deviceLabel(automation.getSensorId()) + " goes "
        + automation.getCondition().name().toLowerCase(Locale.ROOT) + " " + threshold
        + "\n    → send " + automation.getCommand() + " to "
        + model.deviceLabel(automation.getDeviceId());
  }

  /** Fills the reading list with the numeric values the chosen device reports. */
  private void updateReadings() {
    List<String> readings = new ArrayList<>();
    if (model != null && sensor.getValue() != null) {
      model.getDevice(sensor.getValue().id()).ifPresent(d -> {
        for (Map.Entry<String, String> entry : d.getState().entrySet()) {
          try {
            Double.parseDouble(entry.getValue());
            readings.add(entry.getKey());
          } catch (NumberFormatException ignored) {
            // Not a number, so it cannot be compared with a threshold.
          }
        }
      });
    }
    if (!reading.getItems().equals(readings)) {
      String typed = reading.getEditor().getText();
      reading.getItems().setAll(readings);
      if ((typed == null || typed.isBlank()) && !readings.isEmpty()) {
        reading.setValue(readings.get(0));
      }
    }
  }

  /** Fills the command list with the commands the chosen device has announced. */
  private void updateCommands() {
    List<String> commands = new ArrayList<>();
    if (model != null && device.getValue() != null) {
      model.getDevice(device.getValue().id())
          .ifPresent(d -> commands.addAll(d.getCommands().keySet()));
    }
    if (!command.getItems().equals(commands)) {
      String chosen = command.getValue();
      command.getItems().setAll(commands);
      command.setValue(commands.contains(chosen) ? chosen : null);
    }
    updateArgumentHint();
  }

  private void updateArgumentHint() {
    CommandSpec spec = null;
    if (model != null && device.getValue() != null && command.getValue() != null) {
      spec = model.getDevice(device.getValue().id())
          .flatMap(d -> d.getCommand(command.getValue())).orElse(null);
    }
    if (spec == null) {
      argumentHint.setText("");
      argument.setDisable(false);
      return;
    }
    argument.setDisable(spec.getType() == ParameterType.NONE);
    argumentHint.setText(switch (spec.getType()) {
      case NONE -> "No argument needed";
      case BOOLEAN -> "true or false";
      case INTEGER -> "Whole number " + (long) spec.getMin() + "–" + (long) spec.getMax();
      case DECIMAL -> "Number " + spec.getMin() + "–" + spec.getMax();
      case ENUM -> "One of: " + String.join(", ", spec.getOptions());
    });
  }
}
