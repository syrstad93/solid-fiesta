package edu.ntnu.bidata.group1.device.sensor;

import java.util.Random;

/**
 * A number that drifts randomly within a range, used to simulate a sensor's measurements.
 *
 * <p>Each call to {@link #next()} moves the value by a random amount between {@code -maxStep}
 * and {@code +maxStep}, and keeps it between {@code min} and {@code max}. {@link #set(double)}
 * jumps straight to a value, which is useful in demos and tests, for example to push a
 * temperature over an automation's threshold.
 */
public class SimulatedValue {

  private final double min;
  private final double max;
  private final double maxStep;
  private final Random random = new Random();
  private double value;

  /**
   * Creates a simulated value.
   *
   * @param start the first value
   * @param min the smallest value it can drift to
   * @param max the largest value it can drift to
   * @param maxStep the largest change per call to {@link #next()}
   * @throws IllegalArgumentException if {@code min > max} or {@code maxStep < 0}
   */
  public SimulatedValue(double start, double min, double max, double maxStep) {
    if (min > max) {
      throw new IllegalArgumentException("Minimum cannot be greater than maximum.");
    }
    if (maxStep < 0) {
      throw new IllegalArgumentException("Step cannot be negative.");
    }
    this.min = min;
    this.max = max;
    this.maxStep = maxStep;
    this.value = clamp(start);
  }

  /**
   * Moves the value by a random step and returns it.
   *
   * @return the new value
   */
  public synchronized double next() {
    value = clamp(value + (random.nextDouble() * 2 - 1) * maxStep);
    return value;
  }

  /**
   * Returns the current value without changing it.
   *
   * @return the current value
   */
  public synchronized double get() {
    return value;
  }

  /**
   * Sets the value, kept within the range.
   *
   * @param value the new value
   * @return the value actually set, after keeping it within the range
   */
  public synchronized double set(double value) {
    this.value = clamp(value);
    return this.value;
  }

  private double clamp(double v) {
    return Math.max(min, Math.min(max, v));
  }
}
