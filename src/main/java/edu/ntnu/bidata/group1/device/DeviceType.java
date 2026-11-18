package edu.ntnu.bidata.group1.device;

/**
 * The role a {@link Device} has in the system.
 *
 * <p>A device stores its type in the property {@code type}, so it is sent to controllers as
 * {@code type:ACTUATOR}, {@code type:SENSOR} or {@code type:CONTROLLER}.
 */
public enum DeviceType {
  /** A device that can be controlled, for example a light. */
  ACTUATOR,

  /** A device that measures something, for example temperature. */
  SENSOR,

  /** A device that controls other devices. */
  CONTROLLER
}
