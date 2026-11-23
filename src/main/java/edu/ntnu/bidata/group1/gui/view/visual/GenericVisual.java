package edu.ntnu.bidata.group1.gui.view.visual;

import java.util.Map;

import javafx.scene.control.Label;

/**
 * Fallback for devices that none of the other pictures fit: lists the state values as text.
 */
public class GenericVisual extends DeviceVisual {

  private final Label values;

  /** Creates the text display. */
  public GenericVisual() {
    this(new Label());
  }

  private GenericVisual(Label values) {
    super(values);
    this.values = values;
    values.setWrapText(true);
    values.getStyleClass().add("muted");
  }

  @Override
  public void update(Map<String, String> state, boolean online) {
    StringBuilder text = new StringBuilder();
    for (Map.Entry<String, String> entry : state.entrySet()) {
      text.append(entry.getKey()).append(" = ").append(entry.getValue()).append('\n');
    }
    values.setText(text.length() == 0 ? "No state reported yet" : text.toString().trim());
    caption.setText(online ? "" : "Offline");
  }
}
