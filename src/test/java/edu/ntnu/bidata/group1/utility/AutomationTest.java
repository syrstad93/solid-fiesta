package edu.ntnu.bidata.group1.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Automation} and {@link Condition}.
 *
 * <p>Verifies threshold comparison, round-tripping of the request and full string forms,
 * parsing of command values that contain colons, rejection of malformed input and invalid
 * automation definitions, the human-readable description, and device involvement checks.
 */
class AutomationTest {

  // Setup
  private final Automation automation =
      new Automation(
          3,
          "temperature",
          Condition.ABOVE,
          25.0,
          1,
          "power:true"
      );

  /**
   * Verifies that {@link Condition#ABOVE} and {@link Condition#BELOW} compare the sensor reading
   * against the threshold using strict inequality. {@link Condition#ABOVE} is met only when the
   * reading is greater than the threshold, and {@link Condition#BELOW} is met only when the reading
   * is less than the threshold. A reading equal to the threshold meets neither condition.
   */
  @Test
  void conditionsCompareAgainstTheThreshold() {
    assertTrue(Condition.ABOVE.isMet(25.1, 25));
    assertFalse(Condition.ABOVE.isMet(25.0, 25));
    assertTrue(Condition.BELOW.isMet(17.9, 18));
    assertFalse(Condition.BELOW.isMet(18.0, 18));
  }

  /**
   * Verifies that an automation survives a round trip through its request form. The value produced
   * by {@link Automation#toRequestValue()} must be parsed back by
   * {@link Automation#fromRequestValue(String)}, and serializing the parsed automation again must
   * yield the same request value.
   */
  @Test
  void requestFormRoundTrips() {
    String value = automation.toRequestValue();
    assertEquals("3,temperature,ABOVE,25.0,1,power:true", value);
    assertEquals(value, Automation.fromRequestValue(value).toRequestValue());
  }

  /**
   * Verifies that an automation keeps its identifier when it is serialized to its full form and
   * parsed back. The automation is assigned the ID {@code 7}, and its full-form value must match
   * the expected string containing that ID. Parsing that value with
   * {@link Automation#fromFullValue(String)} must produce an automation whose full-form value is
   * identical to the original, and whose {@link Automation#getId()} returns {@code 7}.
   */
  @Test
  void fullFormKeepsTheId() {
    Automation stored = automation.withId(7);
    assertEquals("7,3,temperature,ABOVE,25.0,1,power:true", stored.toFullValue());
    Automation parsed = Automation.fromFullValue(stored.toFullValue());
    assertEquals(stored.toFullValue(), parsed.toFullValue());
    assertEquals(7, parsed.getId());
  }

  /**
   * Verifies that a command value containing colon characters is parsed correctly from the request
   * form. The input {@code "3,humidity,BELOW,30,2,mode:eco"} must yield an automation whose command
   * is {@code "mode:eco"}, with the remaining fields parsed as the condition
   * {@link Condition#BELOW} and a threshold of {@code 30.0}. The colons within the command must not
   * be treated as field separators.
   */
  @Test
  void commandMayContainColons() {
    Automation parsed = Automation.fromRequestValue("3,humidity,BELOW,30,2,mode:eco");
    assertEquals("mode:eco", parsed.getCommand());
    assertEquals(Condition.BELOW, parsed.getCondition());
    assertEquals(30.0, parsed.getThreshold());
  }

  /**
   * Verifies that {@link Automation#fromRequestValue(String)} rejects malformed input by throwing
   * an {@link IllegalArgumentException}. Rejected inputs include a non-numeric identifier, an
   * unknown condition name, a threshold that is not a number, and a {@code null} value.
   */
  @Test
  void malformedValuesAreRejected() {
    assertThrows(IllegalArgumentException.class,
        () -> Automation.fromRequestValue("3,temperature,ABOVE,25"));
    assertThrows(IllegalArgumentException.class,
        () -> Automation.fromRequestValue("x,temperature,ABOVE,25,1,power:true"));
    assertThrows(IllegalArgumentException.class,
        () -> Automation.fromRequestValue("3,temperature,OVER,25,1,power:true"));
    assertThrows(IllegalArgumentException.class,
        () -> Automation.fromRequestValue("3,temperature,ABOVE,warm,1,power:true"));
    assertThrows(IllegalArgumentException.class, () -> Automation.fromRequestValue(null));
  }

  /**
   * Verifies that the {@link Automation} constructor rejects invalid arguments by throwing an
   * {@link IllegalArgumentException}. Rejected inputs include a non-positive identifier, a sensor
   * name containing a colon, a {@code null} condition, a threshold that is not a number, a command
   * containing a slash, and a command that attempts to send a protocol message instead of a device
   * command. An automation may only send device commands, never protocol messages.
   */
  @Test
  void invalidAutomationsAreRejected() {
    assertThrows(IllegalArgumentException.class,
        () -> new Automation(0, "temperature", Condition.ABOVE, 25, 1, "power:true"));
    assertThrows(IllegalArgumentException.class,
        () -> new Automation(3, "temp:c", Condition.ABOVE, 25, 1, "power:true"));
    assertThrows(IllegalArgumentException.class,
        () -> new Automation(3, "temperature", null, 25, 1, "power:true"));
    assertThrows(IllegalArgumentException.class,
        () -> new Automation(3, "temperature", Condition.ABOVE, Double.NaN, 1, "power:true"));
    assertThrows(IllegalArgumentException.class,
        () -> new Automation(3, "temperature", Condition.ABOVE, 25, 1, "a/b"));
    // An automation may only send device commands, never protocol messages.
    assertThrows(IllegalArgumentException.class,
        () -> new Automation(3, "temperature", Condition.ABOVE, 25, 1, "DEVICE-REMOVE:1"));
  }

  /**
   * Verifies that {@link Automation#describe()} returns a sentence describing the automation in
   * plain words. The sentence must name the sensor reading and device, the condition and threshold,
   * and the command and target device. For an automation that sends {@code power:true} to device 1
   * when the temperature reading of device 3 goes above 25.0, the expected description is
   * "When the temperature reading of device 3 goes above 25.0, send power:true to device 1".
   */
  @Test
  void describesItselfInWords() {
    assertEquals("When the temperature reading of device 3 goes above 25.0, send power:true to "
        + "device 1", automation.describe());
  }

  /**
   * Verifies that an automation reports the devices it uses. The automation watches the
   * {@code temperature} reading of sensor 3 but not its {@code humidity} reading. It involves
   * devices 3 and 1, as sensor and target respectively, and does not involve device 2.
   */
  @Test
  void knowsWhichDevicesItUses() {
    assertTrue(automation.watches(3, "temperature"));
    assertFalse(automation.watches(3, "humidity"));
    assertTrue(automation.involves(3));
    assertTrue(automation.involves(1));
    assertFalse(automation.involves(2));
  }
}
