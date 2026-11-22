package edu.ntnu.bidata.group1.gui.view.visual;

import java.util.Map;

import javafx.animation.TranslateTransition;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * A wall switch: knob to the left on a grey track when off, knob to the right on a green track
 * when on. Shows the {@code on} reading a {@link edu.ntnu.bidata.group1.device.sensor.Switch}
 * reports: 1 for on, 0 for off.
 */
public class SwitchVisual extends DeviceVisual {

  private static final Color OFF = Color.web("#4a5060");
  private static final Color ON = Color.web("#34c759");

  private final Rectangle track;
  private final Circle knob;
  private final TranslateTransition slide;
  private Boolean shownOn;

  /** Creates the switch. */
  public SwitchVisual() {
    this(new Rectangle(84, 44), new Circle(17));
  }

  private SwitchVisual(Rectangle track, Circle knob) {
    super(new StackPane(track, knob));
    this.track = track;
    this.knob = knob;
    track.setArcWidth(44);
    track.setArcHeight(44);
    track.setFill(OFF);
    knob.setFill(Color.web("#f2f4f8"));
    knob.setTranslateX(-20);
    slide = new TranslateTransition(Duration.millis(200), knob);
  }

  @Override
  public void update(Map<String, String> state, boolean online) {
    boolean on = number(state, "on", 0) > 0;
    track.setFill(on ? ON : OFF);
    if (shownOn == null || shownOn != on) {
      slide.stop();
      slide.setToX(on ? 20 : -20);
      slide.play();
      shownOn = on;
    }
    String text = on ? "On" : "Off";
    caption.setText(online ? text : text + " · offline");
  }
}
