package edu.ntnu.bidata.group1.gui.view.visual;

import java.util.Map;

import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

/**
 * A padlock: closed and red when locked, open and green when unlocked.
 */
public class DoorLockVisual extends DeviceVisual {

  private static final Color LOCKED = Color.web("#ff5f57");
  private static final Color UNLOCKED = Color.web("#34c759");

  private final SVGPath shackle;
  private final Rectangle body;
  private final TranslateTransition move;
  private Boolean shownLocked;

  /** Creates the padlock. */
  public DoorLockVisual() {
    this(new SVGPath(), new Rectangle(52, 40));
  }

  private DoorLockVisual(SVGPath shackle, Rectangle body) {
    super(build(shackle, body));
    this.shackle = shackle;
    this.body = body;
    move = new TranslateTransition(Duration.millis(250), shackle);
  }

  private static VBox build(SVGPath shackle, Rectangle body) {
    shackle.setContent("M -15 14 L -15 0 A 15 15 0 0 1 15 0 L 15 14");
    shackle.setFill(Color.TRANSPARENT);
    shackle.setStrokeWidth(7);
    shackle.setStrokeLineCap(StrokeLineCap.ROUND);
    body.setArcWidth(10);
    body.setArcHeight(10);
    Circle keyhole = new Circle(5, Color.web("#1e2129"));
    StackPane lockBody = new StackPane(body, keyhole);
    VBox box = new VBox(-6, shackle, lockBody);
    box.setAlignment(Pos.CENTER);
    return box;
  }

  @Override
  public void update(Map<String, String> state, boolean online) {
    boolean locked = !"false".equals(state.get("locked"));
    Color color = locked ? LOCKED : UNLOCKED;
    body.setFill(color);
    shackle.setStroke(color.darker());
    if (shownLocked == null || shownLocked != locked) {
      move.stop();
      move.setToY(locked ? 0 : -12);
      move.play();
      shownLocked = locked;
    }
    String text = locked ? "Locked" : "Unlocked";
    caption.setText(online ? text : text + " · offline");
  }
}
