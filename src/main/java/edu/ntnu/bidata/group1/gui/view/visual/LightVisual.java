package edu.ntnu.bidata.group1.gui.view.visual;

import java.util.Map;

import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * A light bulb: grey when off, glowing yellow when on, brighter the higher its value.
 */
public class LightVisual extends DeviceVisual {

  private static final Color OFF = Color.web("#4a5060");

  private final Circle bulb;
  private final DropShadow glow = new DropShadow();

  /** Creates the bulb. */
  public LightVisual() {
    this(new Circle(28), new Rectangle(22, 14));
  }

  private LightVisual(Circle bulb, Rectangle socket) {
    super(build(bulb, socket));
    this.bulb = bulb;
    bulb.setEffect(glow);
    glow.setSpread(0.25);
  }

  private static VBox build(Circle bulb, Rectangle socket) {
    socket.setArcWidth(6);
    socket.setArcHeight(6);
    socket.setFill(Color.web("#8a90a0"));
    bulb.setStroke(Color.web("#2a2e38"));
    bulb.setStrokeWidth(2);
    VBox box = new VBox(-4, bulb, socket);
    box.setAlignment(Pos.CENTER);
    return box;
  }

  @Override
  public void update(Map<String, String> state, boolean online) {
    boolean on = flag(state, "powerOn") && online;
    long value = Math.round(number(state, "value", 0));
    if (!on) {
      bulb.setFill(OFF);
      glow.setRadius(0);
      glow.setColor(Color.TRANSPARENT);
      caption.setText(online ? "Off" : "Offline");
      return;
    }
    double level = Math.max(0.12, value / 100.0);
    bulb.setFill(Color.hsb(46, 0.35 + 0.55 * level, 0.6 + 0.4 * level));
    glow.setColor(Color.rgb(255, 210, 70, 0.35 + 0.6 * level));
    glow.setRadius(12 + 38 * level);
    caption.setText("On · " + value + " %");
  }
}
