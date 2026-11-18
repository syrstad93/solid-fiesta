package edu.ntnu.bidata.group1.utility;

/**
 * A rule the server follows on its own: when a sensor's reading crosses a threshold, send a
 * command to a device.
 *
 * <p>Example: "when {@code temperature} on device 3 goes {@link Condition#ABOVE} 25, send
 * {@code power:true} to device 1".
 *
 * <p>An automation fires <em>once</em> each time its condition becomes true, not on every
 * reading while it stays true. Once the condition is no longer met, the automation is ready to
 * fire again. To switch something back off, add a second automation with the opposite condition.
 *
 * <p>Automations are sent in messages in two forms:
 * <ul>
 *   <li>Request form, without an id (see {@link #toRequestValue()}):
 *       {@code <sensor>,<reading>,<ABOVE|BELOW>,<threshold>,<device>,<command>}, for example
 *       {@code 3,temperature,ABOVE,25.0,1,power:true}.</li>
 *   <li>Full form, with the id the server gave it (see {@link #toFullValue()}):
 *       {@code <id>,<sensor>,<reading>,<ABOVE|BELOW>,<threshold>,<device>,<command>}.</li>
 * </ul>
 * The command comes last, so it may itself contain {@code :} (as in {@code power:true}).
 *
 * <p>Objects of this class cannot be changed after they are created.
 */
public final class Automation {

  private final long id;
  private final long sensorId;
  private final String reading;
  private final Condition condition;
  private final double threshold;
  private final long deviceId;
  private final String command;

  /**
   * Creates an automation without an id, as sent in an {@code AUTOMATION-ADD} request. The
   * server gives it an id with {@link #withId(long)}.
   *
   * @param sensorId the client id of the sensor to watch
   * @param reading the name of the reading to watch, for example {@code temperature}
   * @param condition whether to fire when the reading goes above or below the threshold
   * @param threshold the threshold
   * @param deviceId the client id of the device to send the command to
   * @param command the command to send, for example {@code power:true}
   * @throws IllegalArgumentException if any value cannot be used or sent in a message
   */
  public Automation(long sensorId, String reading, Condition condition, double threshold,
      long deviceId, String command) {
    this(0L, sensorId, reading, condition, threshold, deviceId, command);
  }

  private Automation(long id, long sensorId, String reading, Condition condition,
      double threshold, long deviceId, String command) {
    if (sensorId <= 0 || deviceId <= 0) {
      throw new IllegalArgumentException("Sensor and device ids must be positive.");
    }
    if (reading == null || reading.isBlank() || containsAny(reading, "/=:,\n\r")) {
      throw new IllegalArgumentException("Invalid reading name: " + reading);
    }
    if (condition == null) {
      throw new IllegalArgumentException("Condition cannot be null.");
    }
    if (Double.isNaN(threshold) || Double.isInfinite(threshold)) {
      throw new IllegalArgumentException("Threshold must be a number.");
    }
    if (command == null || command.isBlank() || containsAny(command, "/\n\r")) {
      throw new IllegalArgumentException("Invalid command: " + command);
    }
    String commandKey = command.split(":", 2)[0].trim();
    if (Protocol.isReservedName(commandKey)) {
      throw new IllegalArgumentException(
          "An automation can only send device commands, not " + commandKey);
    }
    this.id = id;
    this.sensorId = sensorId;
    this.reading = reading.trim();
    this.condition = condition;
    this.threshold = threshold;
    this.deviceId = deviceId;
    this.command = command.trim();
  }

  /**
   * Returns a copy of this automation with the given id.
   *
   * @param id the id the server assigned
   * @return the copy
   */
  public Automation withId(long id) {
    return new Automation(id, sensorId, reading, condition, threshold, deviceId, command);
  }

  /**
   * Returns this automation in request form, without the id.
   *
   * @return for example {@code 3,temperature,ABOVE,25.0,1,power:true}
   */
  public String toRequestValue() {
    return sensorId + "," + reading + "," + condition + "," + threshold + "," + deviceId + ","
        + command;
  }

  /**
   * Returns this automation in full form, with the id first.
   *
   * @return for example {@code 7,3,temperature,ABOVE,25.0,1,power:true}
   */
  public String toFullValue() {
    return id + "," + toRequestValue();
  }

  /**
   * Reads an automation in request form, as produced by {@link #toRequestValue()}.
   *
   * @param value the value of an {@code AUTOMATION-ADD} message
   * @return the automation, without an id
   * @throws IllegalArgumentException if the value is malformed
   */
  public static Automation fromRequestValue(String value) {
    String[] parts = split(value, 6);
    return new Automation(parseId(parts[0], "sensor"), parts[1], parseCondition(parts[2]),
        parseThreshold(parts[3]), parseId(parts[4], "device"), parts[5]);
  }

  /**
   * Reads an automation in full form, as produced by {@link #toFullValue()}.
   *
   * @param value the value of an {@code AUTOMATION-ADDED} message
   * @return the automation, with its id
   * @throws IllegalArgumentException if the value is malformed
   */
  public static Automation fromFullValue(String value) {
    String[] parts = split(value, 7);
    return new Automation(parseId(parts[0], "automation"), parseId(parts[1], "sensor"),
        parts[2], parseCondition(parts[3]), parseThreshold(parts[4]),
        parseId(parts[5], "device"), parts[6]);
  }

  /**
   * Returns whether this automation watches the given sensor reading.
   *
   * @param sensorId the sensor's client id
   * @param reading the reading's name
   * @return {@code true} if it does
   */
  public boolean watches(long sensorId, String reading) {
    return this.sensorId == sensorId && this.reading.equals(reading);
  }

  /**
   * Returns whether this automation uses the given device, as sensor or as target.
   *
   * @param clientId the client id
   * @return {@code true} if it does
   */
  public boolean involves(long clientId) {
    return sensorId == clientId || deviceId == clientId;
  }

  /**
   * Returns a sentence describing the automation.
   *
   * @return for example "When the temperature reading of device 3 goes above 25.0, send
   *     power:true to device 1"
   */
  public String describe() {
    return "When the " + reading + " reading of device " + sensorId + " goes "
        + condition.name().toLowerCase() + " " + threshold + ", send " + command
        + " to device " + deviceId;
  }

  /**
   * Returns the id the server gave this automation.
   *
   * @return the id, or 0 if it has none yet
   */
  public long getId() {
    return id;
  }

  /**
   * Returns the client id of the sensor this automation watches.
   *
   * @return the sensor's id
   */
  public long getSensorId() {
    return sensorId;
  }

  /**
   * Returns the name of the reading this automation watches.
   *
   * @return for example {@code temperature}
   */
  public String getReading() {
    return reading;
  }

  /**
   * Returns whether the automation fires above or below its threshold.
   *
   * @return the condition
   */
  public Condition getCondition() {
    return condition;
  }

  /**
   * Returns the threshold the reading is compared with.
   *
   * @return the threshold
   */
  public double getThreshold() {
    return threshold;
  }

  /**
   * Returns the client id of the device the command is sent to.
   *
   * @return the device's id
   */
  public long getDeviceId() {
    return deviceId;
  }

  /**
   * Returns the whole command, for example {@code power:true}.
   *
   * @return the command
   */
  public String getCommand() {
    return command;
  }

  /**
   * Splits a comma-separated value into the given number of parts. Any commas after the first
   * {@code parts - 1} separators are kept in the last part.
   *
   * @param value the comma-separated value to split
   * @param parts the number of parts the value must split into
   * @return the parts of the value, with exactly {@code parts} elements
   * @throws IllegalArgumentException if the value is {@code null} or does not split into exactly
   *     {@code parts} parts
   */
  private static String[] split(String value, int parts) {
    if (value == null) {
      throw new IllegalArgumentException("Automation cannot be empty.");
    }
    String[] result = value.split(",", parts);
    if (result.length != parts) {
      throw new IllegalArgumentException(
          "Automation needs " + parts + " comma-separated parts, for example "
              + "3,temperature,ABOVE,25,1,power:true");
    }
    return result;
  }

  /**
   * Parses the given text as a numeric id after trimming surrounding whitespace.
   *
   * @param text the text to parse, which should contain a decimal number
   * @param what the kind of id being parsed, used in the error message
   * @return the parsed id
   * @throws IllegalArgumentException if the trimmed text is not a valid {@code long}
   */
  private static long parseId(String text, String what) {
    try {
      return Long.parseLong(text.trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("The " + what + " id must be a number: " + text);
    }
  }

  /**
   * Parses the text of a condition, as used in an {@code AUTOMATION-ADD} request, into a
   * {@link Condition}. Surrounding whitespace is ignored and the name is matched exactly, so
   * {@code ABOVE} and {@code BELOW} are the only accepted values.
   *
   * @param text the text to parse, either {@code ABOVE} or {@code BELOW}
   * @return the condition named by the text
   * @throws IllegalArgumentException if the trimmed text is neither {@code ABOVE} nor {@code BELOW}
   */
  private static Condition parseCondition(String text) {
    try {
      return Condition.valueOf(text.trim());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Condition must be ABOVE or BELOW: " + text);
    }
  }

  /**
   * Parses the given text as a threshold value after trimming surrounding whitespace.
   *
   * @param text the text to parse, which should contain a decimal number
   * @return the parsed threshold
   * @throws IllegalArgumentException if the trimmed text is not a valid {@code double}
   */
  private static double parseThreshold(String text) {
    try {
      return Double.parseDouble(text.trim());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Threshold must be a number: " + text);
    }
  }

  /**
   * Returns whether the given text contains at least one of the characters in the given set.
   *
   * @param text the text to search
   * @param characters the characters to look for, each of which is checked individually
   * @return {@code true} if any character in {@code characters} appears in {@code text},
   *     {@code false} otherwise
   */
  private static boolean containsAny(String text, String characters) {
    for (char c : characters.toCharArray()) {
      if (text.indexOf(c) >= 0) {
        return true;
      }
    }
    return false;
  }
}
