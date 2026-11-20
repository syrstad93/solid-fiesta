package edu.ntnu.bidata.group1.utility;

/**
 * When an {@link Automation} should fire, compared to its threshold.
 */
public enum Condition {

  /** The reading is greater than the threshold. */
  ABOVE,

  /** The reading is less than the threshold. */
  BELOW;

  /**
   * Returns whether the reading meets this condition.
   *
   * @param value the sensor's reading
   * @param threshold the automation's threshold
   * @return {@code true} if the condition is met
   */
  public boolean isMet(double value, double threshold) {
    return switch (this) {
      case ABOVE -> value > threshold;
      case BELOW -> value < threshold;
    };
  }
}
