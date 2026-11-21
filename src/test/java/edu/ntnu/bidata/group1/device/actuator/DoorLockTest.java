package edu.ntnu.bidata.group1.device.actuator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DoorLock}, verifying its initial state, available commands,
 * generated names, and the effect of the supported lock and unlock operations on
 * the device state.
 */
class DoorLockTest {

  /**
   * Test subclass exposing the protected {@link DoorLock#executeCommand(String, String)}
   * method so command execution can be verified directly.
   */
  private static class TestDoorLock extends DoorLock {

    TestDoorLock(String name) {
      super(name);
    }

    void execute(String command) {
      executeCommand(command, "");
    }
  }

  /**
   * Verifies that a newly created {@link DoorLock} starts in the locked state.
   *
   * <p>The constructor initializes the {@code locked} property to {@code true},
   * representing a secure door immediately after creation. Calling
   * {@link DoorLock#isLocked()} is therefore expected to return {@code true}
   * before any commands have been executed.
   */
  @Test
  void newDoorLockStartsLocked() {
    DoorLock doorLock = new DoorLock("front-door");

    assertTrue(doorLock.isLocked());
  }

  /**
   * Verifies that the default constructor generates a non-null name beginning with
   * the documented {@code door-lock} prefix.
   *
   * <p>The exact random suffix is intentionally not checked, since its value is
   * expected to differ between instances. The generated name must however contain
   * the constant prefix so the device can be identified as a door lock.
   */
  @Test
  void defaultConstructorGeneratesDoorLockName() {
    DoorLock doorLock = new DoorLock();

    assertNotNull(doorLock.getName());
    assertTrue(doorLock.getName().startsWith("door-lock"));
  }

  /**
   * Verifies that a {@link DoorLock} registers exactly the two commands it is
   * documented to support.
   *
   * <p>A door lock is expected to provide the commands identified by
   * {@link DoorLock#LOCK} and {@link DoorLock#UNLOCK}. No additional commands
   * should be present.
   */
  @Test
  void availableCommandsContainLockAndUnlock() {
    DoorLock doorLock = new DoorLock("front-door");

    List<CommandSpec> commands = doorLock.getAvailableCommands();

    assertEquals(2, commands.size());

    assertTrue(
        commands.stream()
            .anyMatch(command -> command.getName().equals(DoorLock.LOCK)));

    assertTrue(
        commands.stream()
            .anyMatch(command -> command.getName().equals(DoorLock.UNLOCK)));
  }

  /**
   * Verifies that executing the {@value DoorLock#UNLOCK} command changes the state
   * from locked to unlocked.
   *
   * <p>The device begins in the locked state. After the command has been executed,
   * {@link DoorLock#isLocked()} is expected to return {@code false}.
   */
  @Test
  void unlockCommandUnlocksDoor() {
    TestDoorLock doorLock = new TestDoorLock("front-door");

    doorLock.execute(DoorLock.UNLOCK);

    assertFalse(doorLock.isLocked());
  }

  /**
   * Verifies that executing the {@value DoorLock#LOCK} command places the lock in
   * the locked state, even if it was previously unlocked.
   *
   * <p>The test first unlocks the device and then locks it again. The resulting
   * state is expected to be locked.
   */
  @Test
  void lockCommandLocksDoor() {
    TestDoorLock doorLock = new TestDoorLock("front-door");

    doorLock.execute(DoorLock.UNLOCK);
    assertFalse(doorLock.isLocked());

    doorLock.execute(DoorLock.LOCK);

    assertTrue(doorLock.isLocked());
  }

  /**
   * Verifies that the lock state always reflects the most recently executed
   * command.
   *
   * <p>The sequence unlock, lock and unlock is executed. After each command the
   * device is expected to report the corresponding state through
   * {@link DoorLock#isLocked()}.
   */
  @Test
  void repeatedCommandsUpdateStateCorrectly() {
    TestDoorLock doorLock = new TestDoorLock("front-door");

    assertTrue(doorLock.isLocked());

    doorLock.execute(DoorLock.UNLOCK);
    assertFalse(doorLock.isLocked());

    doorLock.execute(DoorLock.LOCK);
    assertTrue(doorLock.isLocked());

    doorLock.execute(DoorLock.UNLOCK);
    assertFalse(doorLock.isLocked());
  }

  /**
   * Verifies that the {@link DoorLock#LOCK} command constant contains the
   * documented protocol value.
   *
   * <p>The constant should equal the string {@code "lock"}, since this value
   * appears in protocol messages and command specifications.
   */
  @Test
  void lockConstantHasExpectedValue() {
    assertEquals("lock", DoorLock.LOCK);
  }

  /**
   * Verifies that the {@link DoorLock#UNLOCK} command constant contains the
   * documented protocol value.
   *
   * <p>The constant should equal the string {@code "unlock"}, since this value
   * appears in protocol messages and command specifications.
   */
  @Test
  void unlockConstantHasExpectedValue() {
    assertEquals("unlock", DoorLock.UNLOCK);
  }
}