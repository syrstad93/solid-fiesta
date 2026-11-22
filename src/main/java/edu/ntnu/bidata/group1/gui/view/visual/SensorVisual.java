package edu.ntnu.bidata.group1.gui.view.visual;

import java.util.Locale;
import java.util.Map;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * A sensor's readings: the main reading in large text, coloured by its value, and any other
 * readings below.
 */
public class SensorVisual extends DeviceVisual {

  private final Label value;
  private final Label others;

  /** Creates the reading display. */
  public SensorVisual() {
    this(new Label("–"), new Label());
  }

  private SensorVisual(Label value, Label others) {
    super(build(value, others));
    this.value = value;
    this.others = others;
  }

  private static VBox build(Label value, Label others) {
    value.getStyleClass().add("sensor-value");
    others.getStyleClass().add("muted");
    VBox box = new VBox(2, value, others);
    box.setAlignment(Pos.CENTER);
    return box;
  }

  @Override
  public void update(Map<String, String> state, boolean online) {
    value.getStyleClass().removeAll("cold", "comfortable", "warm");
    if (state.isEmpty()) {
      value.setText("–");
      caption.setText("Waiting for readings");
      return;
    }
    String main = state.containsKey("temperature") ? "temperature"
        : state.containsKey("humidity") ? "humidity"
        : state.keySet().iterator().next();
    value.setText(format(main, state.get(main)));
    if (main.equals("temperature")) {
      double celsius = number(state, main, 21);
      value.getStyleClass().add(celsius < 19 ? "cold" : celsius > 23 ? "warm" : "comfortable");
    }
    StringBuilder rest = new StringBuilder();
    for (Map.Entry<String, String> entry : state.entrySet()) {
      if (!entry.getKey().equals(main)) {
        rest.append(entry.getKey()).append(": ")
            .append(format(entry.getKey(), entry.getValue())).append("  ");
      }
    }
    others.setText(rest.toString().trim());
    caption.setText(online ? main : main + " · offline");
  }

  private static String format(String reading, String raw) {
    double number;
    try {
      number = Double.parseDouble(raw);
    } catch (NumberFormatException | NullPointerException e) {
      return String.valueOf(raw);
    }
    return switch (reading) {
      case "temperature" -> String.format(Locale.ROOT, "%.1f °C", number);
      case "humidity" -> String.format(Locale.ROOT, "%.0f %%", number);
      case "on" -> number > 0 ? "ON" : "OFF";
      default -> raw;
    };
  }
}
