package edu.ntnu.bidata.group1.gui.view.visual;

import java.util.Map;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

/**
 * A fan: still and grey when off, blue and spinning when on, faster at higher speeds.
 */
public class FanVisual extends DeviceVisual {

  private static final Color OFF = Color.web("#5a6070");
  private static final Color ON = Color.web("#4fa3ff");

  private final Ellipse[] blades;
  private final RotateTransition spin;

  /** Creates the fan. */
  public FanVisual() {
    this(new Group(), new Ellipse[3]);
  }

  private FanVisual(Group rotor, Ellipse[] blades) {
    super(build(rotor, blades));
    this.blades = blades;
    spin = new RotateTransition(Duration.seconds(1), rotor);
    spin.setByAngle(360);
    spin.setCycleCount(Animation.INDEFINITE);
    spin.setInterpolator(Interpolator.LINEAR);
  }

  private static Group build(Group rotor, Ellipse[] blades) {
    // An invisible circle keeps the rotor's bounds centred, so it spins without wobbling.
    Circle bounds = new Circle(0, 0, 38, Color.TRANSPARENT);
    rotor.getChildren().add(bounds);
    for (int i = 0; i < blades.length; i++) {
      Ellipse blade = new Ellipse(0, -19, 9, 19);
      blade.getTransforms().add(new Rotate(i * 120, 0, 0));
      blade.setFill(OFF);
      blades[i] = blade;
      rotor.getChildren().add(blade);
    }
    Circle hub = new Circle(0, 0, 7, Color.web("#2a2e38"));
    rotor.getChildren().add(hub);
    Circle ring = new Circle(0, 0, 42, Color.TRANSPARENT);
    ring.setStroke(Color.web("#3a4050"));
    ring.setStrokeWidth(3);
    return new Group(ring, rotor);
  }

  @Override
  public void update(Map<String, String> state, boolean online) {
    boolean on = flag(state, "powerOn") && online;
    String speed = state.getOrDefault("speed", "low");
    for (Ellipse blade : blades) {
      blade.setFill(on ? ON : OFF);
    }
    if (!on) {
      spin.pause();
      caption.setText(online ? "Off" : "Offline");
      return;
    }
    spin.setRate(switch (speed) {
      case "high" -> 3.5;
      case "medium" -> 1.8;
      default -> 0.8;
    });
    if (spin.getStatus() != Animation.Status.RUNNING) {
      spin.play();
    }
    caption.setText("On · " + speed);
  }
}
