package edu.ntnu.bidata.group1.utility;

import java.util.regex.Pattern;

/**
 * The role a {@link edu.ntnu.bidata.group1.device.Device} has in the system.
 *
 * <p>A device stores its type in the property {@code type}, so it is sent to controllers as
 * {@code type:ACTUATOR}, {@code type:SENSOR} or {@code type:CONTROLLER}.
 *
 * <p>An actuator or sensor also has a <em>kind</em>: what exactly it is, given as the name of
 * its class, for example {@code Light}, {@code Fan} or {@code TemperatureSensor}. It is sent in
 * {@code HELLO}, and the server passes it on in {@code DEVICE-ADDED}, so a controller knows what
 * a device is even while it is offline. See {@link #isValidKind(String)}.
 */
public enum DeviceType {
  /** A device that can be controlled, for example a light. */
  ACTUATOR,

  /** A device that measures something, for example temperature. */
  SENSOR,

  /** A device that controls other devices. */
  CONTROLLER;

  /** UpperCamelCase with at least one lower-case letter, like a Java class name. */
  private static final Pattern KIND = Pattern.compile("[A-Z][A-Za-z0-9]*[a-z][A-Za-z0-9]*");

  /**
   * Returns whether a text can be used as a device kind: a class name in UpperCamelCase, such
   * as {@code Fan} or {@code DoorLock}. Names in capitals only, such as {@code OWNER}, are not
   * kinds, so a kind can never be mistaken for a {@link Role}.
   *
   * @param kind the text to check
   * @return {@code true} if it is a valid kind
   */
  public static boolean isValidKind(String kind) {
    return kind != null && KIND.matcher(kind).matches();
  }
}
