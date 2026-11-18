package edu.ntnu.bidata.group1.device.sensor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SimulatedValue} and the reading name validation in {@link Sensor}.
 *
 * The tests check that a simulated value stays within its range and never changes by more
 * than its maximum step on each call to {@link SimulatedValue#next()}, that
 * {@link SimulatedValue#set(double)} clamps values to the range, that invalid constructor
 * arguments are rejected, and that {@link Sensor#checkReadingName(String)} accepts valid
 * names and rejects invalid ones.
 */
class SimulatedValueTest {

  /**
   * Verifies that repeated calls to {@link SimulatedValue#next()} keep the value within its
   * configured range and that no single call changes the value by more than the configured
   * maximum step.
   *
   * The test starts a {@link SimulatedValue} with a range of 0 to 100 and a maximum step of 2,
   * then draws 1000 successive values. For each value it checks that the value lies within the
   * range, and that its distance from the previous value does not exceed the maximum step, with
   * a small tolerance for floating-point rounding.
   */
  @Test
  void driftsWithinRangeAndStep() {
    SimulatedValue value = new SimulatedValue(50, 0, 100, 2);
    double previous = value.get();
    for (int i = 0; i < 1000; i++) {
      double next = value.next();
      assertTrue(next >= 0 && next <= 100, "out of range: " + next);
      assertTrue(Math.abs(next - previous) <= 2.0 + 1e-9, "step too large: " + next);
      previous = next;
    }
  }

  /**
   * Verifies that {@link SimulatedValue#set(double)} clamps the new value to the configured range.
   *
   * The test creates a {@link SimulatedValue} with a range of -10 to 40. It checks that a value
   * above the maximum is set to the maximum, that a value below the minimum is set to the minimum,
   * and that a value inside the range is set unchanged. In each case the value returned by
   * {@code set} is checked.
   */
  @Test
  void setKeepsTheValueInRange() {
    SimulatedValue value = new SimulatedValue(21, -10, 40, 0.3);
    assertEquals(40.0, value.set(100));
    assertEquals(-10.0, value.set(-50));
    assertEquals(25.0, value.set(25));
  }

  /**
   * Verifies that the {@link SimulatedValue} constructor rejects invalid arguments.
   *
   * The test checks that a range whose minimum is greater than its maximum is rejected, and
   * that a negative maximum step is rejected. In both cases an {@link IllegalArgumentException}
   * is expected.
   */
  @Test
  void invalidRangesAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> new SimulatedValue(0, 10, 0, 1));
    assertThrows(IllegalArgumentException.class, () -> new SimulatedValue(0, 0, 10, -1));
  }

  /**
   * Verifies that {@link Sensor#checkReadingName(String)} accepts a plain reading name and
   * rejects names that cannot be sent in a message.
   *
   * <p>The test expects {@code temperature} to be accepted. It expects an
   * {@link IllegalArgumentException} for a name containing a comma ({@code a,b}) or a colon
   * ({@code a:b}), for a blank name, and for {@code error}, which is reserved by the protocol.
   */
  @Test
  void readingNamesAreChecked() {
    Sensor.checkReadingName("temperature");
    assertThrows(IllegalArgumentException.class, () -> Sensor.checkReadingName("a,b"));
    assertThrows(IllegalArgumentException.class, () -> Sensor.checkReadingName("a:b"));
    assertThrows(IllegalArgumentException.class, () -> Sensor.checkReadingName(" "));
    assertThrows(IllegalArgumentException.class, () -> Sensor.checkReadingName("error"));
  }
}
