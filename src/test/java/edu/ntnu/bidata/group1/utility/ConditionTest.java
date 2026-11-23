package edu.ntnu.bidata.group1.utility;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for the Condition enum.
 */
class ConditionTest {

  /**
   * Tests that ABOVE is met when the value is greater than the threshold.
   */
  @Test
  void aboveReturnsTrueWhenValueIsGreaterThanThreshold() {
    assertTrue(Condition.ABOVE.isMet(26.0, 25.0));
  }
}