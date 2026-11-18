package edu.ntnu.bidata.group1.device.actuator;

/**
 * Actuator that represents an electronic door lock. The lock starts in the locked state and
 * accepts the {@value #LOCK} and {@value #UNLOCK} commands, which change whether the door is
 * locked. The current state is kept in the device's {@code locked} property, which is published
 * to the server whenever it changes.
 *
 * <p>Use {@link #isLocked()} to read the current state. Commands are carried out by
 * {@link #executeCommand(String, String)}, after they have been validated against their
 * specifications.
 *
 * @see Actuator
 */
public class DoorLock extends Actuator {

  /** Valid commands */
  public static final String LOCK = "lock";
  public static final String UNLOCK = "unlock";

  /**
   * Creates a door lock with a random name, such as {@code door-lock-3f9a1c2e}. See
   * {@link #DoorLock(String)}.
   */
  public DoorLock() {
    this(randomName("door-lock"));
  }

  /**
   * Creates a door lock that is initially locked. Call {@link #start()} to connect it.
   *
   * @param name the device's name, used to introduce it to the server, for example
   *     {@code kitchen-door-lock}
   * @throws IllegalArgumentException if the name is not allowed
   */
  public DoorLock(String name) {
    super(name);
    properties.put("locked", true);
    registerCommand(CommandSpec.noParameter(LOCK));
    registerCommand(CommandSpec.noParameter(UNLOCK));
  }

  @Override
  protected void executeCommand(String name, String argument) {
    switch (name) {
      case LOCK -> properties.put("locked", true);
      case UNLOCK -> properties.put("locked", false);
      default -> { }
    }
  }

  /**
   * Returns whether the door is locked.
   *
   * @return {@code true} if locked
   */
  public boolean isLocked() {
    return (boolean) properties.get("locked");
  }
}
