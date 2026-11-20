package edu.ntnu.bidata.group1.network.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ntnu.bidata.group1.utility.Automation;
import edu.ntnu.bidata.group1.utility.Condition;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link AutomationEngine}.
 *
 * <p>The tests cover registering automations and assigning their identifiers, edge-triggered firing
 * when a condition becomes true, filtering of readings by sensor and reading name, evaluating
 * automations against the latest recorded reading, and removing automations.
 */
class AutomationEngineTest {

  private static final long SENSOR = 3;
  private static final long LIGHT = 1;

  private AutomationEngine engine;
  private Automation tooWarm;

  /**
   * Prepares a fresh {@link AutomationEngine} and registers a default automation before each test.
   *
   * <p>The registered automation watches the temperature reading of the sensor and turns on the
   * light when the value rises above 25. The stored automation is kept in the {@code tooWarm}
   * field so that tests can refer to it.
   */
  @BeforeEach
  void setUp() {
    engine = new AutomationEngine();
    tooWarm = engine.add(
        new Automation(SENSOR, "temperature", Condition.ABOVE, 25, LIGHT, "power:true"));
  }

  /**
   * Verifies that automations added to the engine receive sequentially increasing identifiers.
   *
   * <p>The automation registered in {@code setUp()} is expected to receive the first identifier,
   * and a second automation added afterwards is expected to receive the next one. The test also
   * checks that the engine returns all registered automations in insertion order.
   */
  @Test
  void addGivesIncreasingIds() {
    Automation second = engine.add(
        new Automation(SENSOR, "temperature", Condition.BELOW, 18, LIGHT, "power:false"));
    assertEquals(1, tooWarm.getId());
    assertEquals(2, second.getId());
    assertEquals(List.of(tooWarm, second), engine.getAll());
  }

  /**
   * Verifies that an automation fires exactly once when its condition transitions from false to
   * true.
   *
   * <p>The test feeds a series of temperature readings to the engine and checks that the
   * automation is reported only on the reading that makes its condition become true. While the
   * condition remains true, further readings do not trigger the automation again. After the
   * reading drops back below the threshold, a subsequent rise makes the condition true again and
   * the automation fires once more.
   */
  @Test
  void firesOnceWhenTheConditionBecomesTrue() {
    assertTrue(engine.onReading(SENSOR, "temperature", 24).isEmpty());
    assertEquals(List.of(tooWarm), engine.onReading(SENSOR, "temperature", 26));
    // Still above: does not fire again.
    assertTrue(engine.onReading(SENSOR, "temperature", 27).isEmpty());
    // Drops below, then rises again: fires again.
    assertTrue(engine.onReading(SENSOR, "temperature", 24).isEmpty());
    assertEquals(List.of(tooWarm), engine.onReading(SENSOR, "temperature", 26));
  }

  /**
   * Verifies that readings which do not match the automation's watched sensor and reading name
   * do not trigger it.
   *
   * <p>The registered automation watches the temperature reading of {@code SENSOR}. The test
   * sends a humidity reading from {@code SENSOR} and a temperature reading from a different sensor,
   * and checks that neither produces any fired automations.
   */
  @Test
  void ignoresOtherSensorsAndReadings() {
    assertTrue(engine.onReading(SENSOR, "humidity", 99).isEmpty());
    assertTrue(engine.onReading(4, "temperature", 99).isEmpty());
  }

  /**
   * Verifies that {@link AutomationEngine#evaluateNow(long)} evaluates an automation against the
   * most recently recorded reading for its watched sensor and reading name.
   *
   * <p>A temperature reading of 30 is recorded before an automation that fires when the temperature
   * is above 28 is added. Evaluating the automation immediately returns that automation, and the
   * latest reading for the sensor's temperature is reported as 30.0. The automation is then
   * considered fired, so a subsequent reading of 31, which is still above the threshold, does not
   * fire it again.
   */
  @Test
  void evaluateNowUsesTheLatestReading() {
    engine.onReading(SENSOR, "temperature", 30);
    Automation added = engine.add(
        new Automation(SENSOR, "temperature", Condition.ABOVE, 28, LIGHT, "value:10"));
    assertEquals(List.of(added), engine.evaluateNow(added.getId()));
    assertEquals(30.0, engine.latestReading(SENSOR, "temperature"));
    // Already fired; the next reading above the threshold does not fire it again.
    assertTrue(engine.onReading(SENSOR, "temperature", 31).stream()
        .noneMatch(a -> a.getId() == added.getId()));
  }

  /**
   * Verifies that {@link AutomationEngine#evaluateNow(long)} does nothing when no reading has been
   * recorded for the automation's watched sensor and reading name.
   *
   * <p>The test calls {@code evaluateNow} for the automation stored in {@code tooWarm} before any
   * reading has been sent. It checks that the returned list is empty, so no automation is reported
   * as fired. It also checks that {@link AutomationEngine#latestReading(long, String)} returns
   * {@code NaN} for the sensor's temperature, confirming that no value was stored.
   */
  @Test
  void evaluateNowWithoutReadingsDoesNothing() {
    assertTrue(engine.evaluateNow(tooWarm.getId()).isEmpty());
    assertTrue(Double.isNaN(engine.latestReading(SENSOR, "temperature")));
  }

  /**
   * Verifies that removing an automation stops it from being evaluated and that it cannot be
   * removed again.
   *
   * <p>The automation stored in {@code tooWarm} is removed from the engine. A subsequent
   * temperature reading of 30 from {@code SENSOR}, which would otherwise make the automation fire,
   * is expected to return no fired automations. Attempting to remove the same automation a second
   * time is expected to throw an {@link IllegalArgumentException}, because no automation with its
   * id remains registered.
   */
  @Test
  void removeStopsTheAutomation() {
    engine.remove(tooWarm.getId());
    assertTrue(engine.onReading(SENSOR, "temperature", 30).isEmpty());
    assertThrows(IllegalArgumentException.class, () -> engine.remove(tooWarm.getId()));
  }

}
