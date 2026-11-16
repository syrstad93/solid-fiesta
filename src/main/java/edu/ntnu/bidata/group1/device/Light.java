package edu.ntnu.bidata.group1.device;

import edu.ntnu.bidata.group1.network.ClientConnectionHolder;
import edu.ntnu.bidata.group1.utility.MessageWorker;

/**
 * Actuator device that controls a light by its power state and brightness value.
 *
 * <p>A light is a {@link Device} of type {@link Type#ACTUATOR}. Its brightness value is a
 * percentage in the inclusive range from 0 to 100. Incoming commands can set the identifier,
 * the power state, and the brightness value of the light.
 */
public class Light extends Device {


  /**
   * Creates a light actuator that is initially powered off.
   *
   * <p>The light is registered with the {@link Type#ACTUATOR} device type, and its power state
   * is set to off.
   *
   * @throws Exception if the underlying device cannot be initialized
   */
  public Light() throws Exception {
      super(Type.ACTUATOR);
      long value = 0;
      boolean powerOn = false;
      properties.put("value", value);
      properties.put("powerOn", powerOn);
      messageDaemon();
  }

  /**
   * Sets the brightness value of this light, clamped to the inclusive range from 0 to 100.
   *
   * <p>Values greater than 100 are stored as 100, and values less than 0 are stored as 0.
   *
   * @param value the requested brightness value as a percentage
   */
  private void setValue(long value) {
    if (value <= 100L && value >= 0L ) {
      this.properties.put("value", value);
      this.properties.put("powerOn", true);

    } else if (value >= 100L) {
      this.properties.put("value", value);
      this.properties.put("powerOn", true);
    }  else {
      this.properties.put("value", 0L);
    }
  }

  private long getValue() {
    return ((long) this.properties.get("value"));
  }

  private boolean getPowerOn() {
    return ((boolean) this.properties.get("powerOn"));
  }

  /**
   * Sets the power state of this light.
   *
   * <p>The power state is {@code true} when the light is turned on and {@code false} when it is
   * turned off. The brightness value is not changed by this method.
   *
   * @param powerOn {@code true} to power the light on, {@code false} to power it off
   */
  private void setPowerOn(boolean powerOn) {
    this.properties.put("powerOn", powerOn);
  }

  /**
   * Handles a text message received from a client connection.
   *
   * <p>The message is parsed into a command of the form {@code key:value}. The recognized keys
   * are {@code id}, which sets the device identifier, {@code power}, which sets the power state,
   * and {@code value}, which sets the brightness value. Messages without a separator are ignored.
   *
   * @param message the raw message text received from the client
   * @param sender the connection holder of the client that sent the message
   */
  @Override
  public void onMessage(String message, ClientConnectionHolder sender) {
    MessageWorker mw = new MessageWorker(message);

    String cmd = mw.getCommand();
    int separator = cmd.indexOf(':');
    if (separator < 0) {
      return;
    }

    String argument = cmd.substring(separator + 1).trim();
    switch (cmd.substring(0, separator).trim()) {
      case "id" -> this.setId(Long.parseLong(argument));
      case "power" -> this.setPowerOn(Boolean.parseBoolean(argument));
      case "value" -> this.setValue(Long.parseLong(argument));
      default -> { }
    }
  }
}