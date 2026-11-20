package edu.ntnu.bidata.group1.device.sensor;

import edu.ntnu.bidata.group1.device.Device;
import edu.ntnu.bidata.group1.network.ClientConnectionHolder;
import edu.ntnu.bidata.group1.utility.DeviceType;
import edu.ntnu.bidata.group1.utility.MessageWorker;
import edu.ntnu.bidata.group1.utility.Protocol;
import java.util.Optional;

/**
 * Base class for devices that measure something: temperature, humidity, light level and so on.
 *
 * <p>A sensor measures every {@value #MEASURE_INTERVAL_MS} milliseconds once it has been
 * started with {@link #start()}. Each measurement is reported with
 * {@link #report(String, double)}, which stores it as a property and, if the value changed,
 * sends it to the server as {@code STATE:<reading>,<value>}. The server passes it on to the
 * sensor's subscribers and checks it against the automations.
 *
 * <p>Sensors accept no commands. They answer {@code GET-COMMANDS} with {@code COMMAND-END:0}, and
 * any device command with {@code ERROR}.
 *
 * <h2>Writing a sensor</h2>
 * <pre>{@code
 * public class LightLevelSensor extends Sensor {
 *   private final SimulatedValue lux = new SimulatedValue(300, 0, 1000, 20);
 *
 *   public LightLevelSensor(String name) {
 *     super(name);
 *   }
 *
 *   protected void measure() {
 *     report("lux", lux.next());
 *   }
 * }
 * }</pre>
 */
public abstract class Sensor extends Device {

  /** How often the sensor measures and reports, in milliseconds. */
  public static final long MEASURE_INTERVAL_MS = 2000;

  /**
   * Creates a sensor with the given name. It is connected, and starts measuring, when
   * {@link #start()} is called.
   *
   * @param name the sensor's name, for example {@code living-room-temperature}
   * @throws IllegalArgumentException if the name is not allowed
   */
  protected Sensor(String name) {
    super(name, DeviceType.SENSOR);
  }

  /**
   * Takes one measurement and reports it with {@link #report(String, double)}. Called every
   * {@value #MEASURE_INTERVAL_MS} milliseconds.
   */
  protected abstract void measure();

  /**
   * Sends the sensor's whole state to the server, then starts a background thread that calls
   * {@link #measure()} every {@value #MEASURE_INTERVAL_MS} milliseconds. Measures once right
   * away. Runs once, when the sensor is started.
   */
  @Override
  protected void onStarted() {
    super.onStarted();
    Thread measurer = new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        measure();
        try {
          Thread.sleep(MEASURE_INTERVAL_MS);
        } catch (InterruptedException e) {
          return;
        }
      }
    });
    measurer.setName(getName() + "-measurer");
    measurer.setDaemon(true);
    measurer.start();
  }

  /**
   * Stores a measurement and, if it differs from the last one, sends it to the server as
   * {@code STATE:<reading>,<value>}. The value is rounded to one decimal. Before the sensor is
   * started, the value is only stored, and sent when it starts.
   *
   * @param reading the name of what was measured, for example {@code temperature}; lower case,
   *     and must not contain {@code / = : ,} or be a protocol key
   * @param value the measured value
   * @throws IllegalArgumentException if the reading name is not allowed or the value is not a
   *     number
   */
  protected final void report(String reading, double value) {
    checkReadingName(reading);
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      throw new IllegalArgumentException("A reading must be a number.");
    }
    double rounded = Math.round(value * 10.0) / 10.0;
    Object previous = properties.put(reading, rounded);
    if (!Double.valueOf(rounded).equals(previous)) {
      publish(reading);
    }
  }

  /**
   * Returns the last reported value of the given reading.
   *
   * @param reading the reading's name, for example {@code temperature}
   * @return the value, or empty if nothing has been reported yet
   */
  public Optional<Double> getReading(String reading) {
    Object value = properties.get(reading);
    return value instanceof Double d ? Optional.of(d) : Optional.empty();
  }

  @Override
  public final void onMessage(String message, ClientConnectionHolder sender) {
    MessageWorker mw;
    try {
      mw = new MessageWorker(message);
    } catch (IllegalArgumentException e) {
      return;
    }
    String replyTo = mw.getSource();
    Optional<Protocol> protocol = mw.getProtocol();

    if (protocol.isEmpty()) {
      sendError(replyTo, mw.getCommandKey(), "a sensor accepts no commands");
      return;
    }
    if (handleCommonRequest(protocol.get(), mw.getCommandValue(), replyTo)) {
      return;
    }
    if (protocol.get() == Protocol.COMMANDS_REQUEST) {
      sendMessage(replyTo, Protocol.COMMAND_END.withValue(0));
    }
    // Replies, announcements and server requests are not for sensors.
  }

  /**
   * Checks that a reading name can be sent in a message.
   *
   * @param reading the reading's name
   * @throws IllegalArgumentException if it cannot
   */
  static void checkReadingName(String reading) {
    if (reading == null || reading.isBlank()) {
      throw new IllegalArgumentException("Reading name cannot be empty.");
    }
    for (char c : "/=:,".toCharArray()) {
      if (reading.indexOf(c) >= 0) {
        throw new IllegalArgumentException("Reading name cannot contain '" + c + "': " + reading);
      }
    }
    if (Protocol.isReservedName(reading)) {
      throw new IllegalArgumentException("Reading name is reserved by the protocol: " + reading);
    }
  }
}
