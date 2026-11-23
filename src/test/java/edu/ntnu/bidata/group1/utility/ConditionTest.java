package edu.ntnu.bidata.group1.utility;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Condition enum..
 */
class ConditionTest {

  /**
   * Tests that ABOVE is met when the value is greater than the threshold.
   */
  @Test
  void aboveReturnsTrueWhenValueIsGreaterThanThreshold() {
    assertTrue(Condition.ABOVE.isMet(26.0, 25.0));
  }

  /**
   * Tests that ABOVE is not met when the value is less than the threshold.
   */
  @Test
  void aboveReturnsFalseWhenValueIsLessThanThreshold() {
    assertFalse(Condition.ABOVE.isMet(24.0, 25.0));
  }

  /**
   * Tests that ABOVE is not met when the value equals the threshold.
   */
  @Test
  void aboveReturnsFalseWhenValueEqualsThreshold() {
    assertFalse(Condition.ABOVE.isMet(25.0, 25.0));
  }

  /**
   * Tests that BELOW is met when the value is less than the threshold.
   */
  @Test
  void belowReturnsTrueWhenValueIsLessThanThreshold() {
    assertTrue(Condition.BELOW.isMet(24.0, 25.0));
  }

  /**
   * Tests that BELOW is not met when the value is greater than the threshold.
   */
  @Test
  void belowReturnsFalseWhenValueIsGreaterThanThreshold() {
    assertFalse(Condition.BELOW.isMet(26.0, 25.0));
  }

  /**
   * Tests that BELOW is not met when the value equals the threshold.
   */
  @Test
  void belowReturnsFalseWhenValueEqualsThreshold() {
    assertFalse(Condition.BELOW.isMet(25.0, 25.0));
  }
}