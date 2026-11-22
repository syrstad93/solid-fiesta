package edu.ntnu.bidata.group1.gui.view.visual;

import java.util.Locale;
import java.util.Map;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

/**
 * A radiator: grey when off; orange to red when on, redder the higher the target temperature,
 * with heat waves rising above it.
 */
public class HeaterVisual extends DeviceVisual {

  private static final Color OFF = Color.web("#5a6070");

  private final Rectangle[] fins;
  private final HBox waves;
  private final DropShadow glow = new DropShadow();
  private final FadeTransition shimmer;

  /** Creates the radiator. */
  public HeaterVisual() {
    this(new Rectangle[5], new HBox(10));
  }

  private HeaterVisual(Rectangle[] fins, HBox waves) {
    super(build(fins, waves));
    this.fins = fins;
    this.waves = waves;
    shimmer = new FadeTransition(Duration.seconds(0.9), waves);
    shimmer.setFromValue(0.25);
    shimmer.setToValue(1.0);
    shimmer.setAutoReverse(true);
    shimmer.setCycleCount(Animation.INDEFINITE);
  }

  private static VBox build(Rectangle[] fins, HBox waves) {
    for (int i = 0; i < 3; i++) {
      SVGPath wave = new SVGPath();
      wave.setContent("M0 18 Q 5 12 0 6 Q -5 0 0 -6");
      wave.setStroke(Color.web("#ff8a50"));
      wave.setStrokeWidth(2.5);
      wave.setFill(Color.TRANSPARENT);
      waves.getChildren().add(wave);
    }
    waves.setAlignment(Pos.CENTER);
    waves.setOpacity(0);
    HBox body = new HBox(4);
    body.setAlignment(Pos.CENTER);
    for (int i = 0; i < fins.length; i++) {
      Rectangle fin = new Rectangle(11, 52);
      fin.setArcWidth(8);
      fin.setArcHeight(8);
      fin.setFill(OFF);
      fins[i] = fin;
      body.getChildren().add(fin);
    }
    VBox box = new VBox(4, waves, body);
    box.setAlignment(Pos.CENTER);
    return box;
  }

  @Override
  public void update(Map<String, String> state, boolean online) {
    boolean on = flag(state, "powerOn") && online;
    double target = number(state, "targetTemperature", 21.0);
    String targetText = String.format(Locale.ROOT, "%.1f °C", target);
    if (!on) {
      for (Rectangle fin : fins) {
        fin.setFill(OFF);
        fin.setEffect(null);
      }
      shimmer.stop();
      waves.setOpacity(0);
      caption.setText((online ? "Off" : "Offline") + " · set " + targetText);
      return;
    }
    double heat = Math.min(1, Math.max(0, (target - 5) / 25));
    Color color = Color.web("#ffb347").interpolate(Color.web("#ff3b30"), heat);
    glow.setColor(color.deriveColor(0, 1, 1, 0.7));
    glow.setRadius(10 + 20 * heat);
    for (Rectangle fin : fins) {
      fin.setFill(color);
      fin.setEffect(glow);
    }
    if (shimmer.getStatus() != Animation.Status.RUNNING) {
      shimmer.play();
    }
    caption.setText("Heating to " + targetText);
  }
}
