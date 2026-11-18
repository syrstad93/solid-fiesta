package edu.ntnu.bidata.group1.device.sensor;

/**
 * Sensor for an on/off switch, such as a wall switch or a door contact.
 *
 * <p>Reports the reading {@value #READING}: {@code 1} when the switch is on and {@code 0} when it
 * is off. A change is sent to the server right away.
 *
 * <p>Because the reading is a number, a switch can drive automations like any other sensor. Use
 * a threshold between 0 and 1:
 * <ul>
 *   <li>{@code on ABOVE 0.5} fires when the switch is turned on.</li>
 *   <li>{@code on BELOW 0.5} fires when the switch is turned off.</li>
 * </ul>
 * For example {@code AUTOMATION-ADD:5,on,ABOVE,0.5,1,power:true} turns on device 1 when switch 5
 * is turned on.
 *
 * <p>The switch starts in the off position. Its position is changed in code with
 * {@link #turnOn()}, {@link #turnOff()} and {@link #toggle()}, standing in for someone pressing
 * the real switch; it cannot be changed through messages, because sensors accept no commands.
 */
public class Switch extends Sensor {

  /** Name of the reading this sensor reports. */
  public static final String READING = "on";

  private volatile boolean on;

  /**
   * Creates a switch with a random name, such as {@code switch-3f9a1c2e}. See
   * {@link #Switch(String)}.
   */
  public Switch() {
    this(randomName("switch"));
  }

  /**
   * Creates a switch in the off position. Call {@link #start()} to
   * connect it and start reporting.
   *
   * @param name the device's name, used to introduce it to the server, for example
   *     {@code kitchen-switch}
   * @throws IllegalArgumentException if the name is not allowed
   */
  public Switch(String name) {
    super(name);
  }

  @Override
  protected void measure() {
    report(READING, on ? 1 : 0);
  }

  /**
   * Turns the switch on and reports it right away.
   */
  public void turnOn() {
    setOn(true);
  }

  /**
   * Turns the switch off and reports it right away.
   */
  public void turnOff() {
    setOn(false);
  }

  /**
   * Flips the switch and reports the new position right away.
   */
  public synchronized void toggle() {
    setOn(!on);
  }

  /**
   * Returns whether the switch is on.
   *
   * @return {@code true} if on
   */
  public boolean isOn() {
    return on;
  }

  private synchronized void setOn(boolean on) {
    this.on = on;
    measure();
  }
}
