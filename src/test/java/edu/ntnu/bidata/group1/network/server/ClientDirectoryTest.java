package edu.ntnu.bidata.group1.network.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ntnu.bidata.group1.utility.DeviceType;
import edu.ntnu.bidata.group1.utility.Role;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link ClientDirectory}.
 *
 * <p>The tests cover parsing of {@code HELLO} values into {@link ClientDirectory.Client} records,
 * rejection of malformed values, acceptance of every {@link DeviceType} and {@link Role} constant,
 * and the id bookkeeping that keeps a client's id stable when it reconnects with the same name.
 * They also check how unnamed clients are reported, using their address instead of a name.
 *
 * <p>Each test creates its own directory, so tests do not depend on each other or on test order.
 */
class ClientDirectoryTest {

  /**
   * Verifies that {@link ClientDirectory#parseHello(String)} reads the name, type and role from a
   * {@code HELLO} value.
   *
   * <p>A client that is not a controller, such as an actuator, is parsed with a {@code null} role.
   * A controller is parsed with the role it gives, such as {@code OWNER}.
   */
  @Test
  void parsesDevicesAndControllers() {
    assertEquals(new ClientDirectory.Client("kitchen-light", DeviceType.ACTUATOR, null),
        ClientDirectory.parseHello("kitchen-light,ACTUATOR"));
    assertEquals(new ClientDirectory.Client("anna-phone", DeviceType.CONTROLLER, Role.OWNER),
        ClientDirectory.parseHello("anna-phone,CONTROLLER,OWNER"));
  }

  /**
   * Verifies that {@link ClientDirectory#parseHello(String)} rejects every malformed {@code HELLO}
   * value by throwing an {@link IllegalArgumentException}.
   *
   * <p>Rejected values include a missing value, a value with no type, a value with an empty name,
   * an unknown client type, a controller with no role or an unknown role, a non-controller that
   * gives a role, and a type or role written in the wrong case.
   */
  @Test
  void rejectsMalformedHellos() {
    assertThrows(IllegalArgumentException.class, () -> ClientDirectory.parseHello(null));
    assertThrows(IllegalArgumentException.class, () -> ClientDirectory.parseHello("lamp"));
    assertThrows(IllegalArgumentException.class, () -> ClientDirectory.parseHello(",ACTUATOR"));
    assertThrows(IllegalArgumentException.class, () -> ClientDirectory.parseHello("lamp,TOASTER"));
    assertThrows(IllegalArgumentException.class,
        () -> ClientDirectory.parseHello("phone,CONTROLLER"));
    assertThrows(IllegalArgumentException.class,
        () -> ClientDirectory.parseHello("phone,CONTROLLER,ADMIN"));
    assertThrows(IllegalArgumentException.class,
        () -> ClientDirectory.parseHello("lamp,ACTUATOR,OWNER"));
    assertThrows(IllegalArgumentException.class,
        () -> ClientDirectory.parseHello("lamp,actuator"));
  }

  /**
   * Verifies that {@link ClientDirectory#parseHello(String)} accepts every constant of
   * {@link DeviceType} except {@link DeviceType#CONTROLLER}, and every constant of {@link Role}
   * for a controller.
   *
   * <p>Each non-controller type must be parsed back as the same type. Each role must be parsed back
   * as the same role when the client is a controller. This keeps the parser in step with the enums,
   * so that adding a constant without updating the parser makes this test fail.
   */
  @Test
  void acceptsEveryTypeAndRoleInTheEnums() {
    for (DeviceType type : DeviceType.values()) {
      if (type != DeviceType.CONTROLLER) {
        assertEquals(type, ClientDirectory.parseHello("client," + type).type());
      }
    }
    for (Role role : Role.values()) {
      assertEquals(role, ClientDirectory.parseHello("phone,CONTROLLER," + role).role());
    }
  }

  /**
   * Verifies that a client introducing itself with a name not seen before keeps the id of its
   * connection.
   *
   * <p>The client {@code lamp}, an actuator, connects on connection id {@code 4} and sends its
   * {@code HELLO}. The directory must return {@code 4} as the id, report that the id is named, and
   * report {@code lamp} as its name.
   */
  @Test
  void newNameKeepsTheConnectionsId() {
    ClientDirectory directory = new ClientDirectory();
    assertEquals(4, directory.introduce(ClientDirectory.parseHello("lamp,ACTUATOR"), 4));
    assertTrue(directory.isNamed(4));
    assertEquals("lamp", directory.nameOf(4));
  }

  /**
   * Verifies that a client introducing itself with a name the directory has already seen gets its
   * old id back, not the id of its new connection.
   *
   * <p>The client {@code lamp}, an actuator, first connects on connection id {@code 4} and sends
   * its {@code HELLO}. It then reconnects on connection id {@code 9} and sends the same
   * {@code HELLO}. The directory must return {@code 4} the second time. It must also report that
   * connection {@code 9} is not named, because the name and its client are kept under {@code 4}.
   */
  @Test
  void knownNameGetsItsOldIdBack() {
    ClientDirectory directory = new ClientDirectory();
    directory.introduce(ClientDirectory.parseHello("lamp,ACTUATOR"), 4);
    assertEquals(4, directory.introduce(ClientDirectory.parseHello("lamp,ACTUATOR"), 9));
    assertFalse(directory.isNamed(9));
  }

  /**
   * Verifies that a client which has not introduced itself with a name is reported as unnamed,
   * has no {@link ClientDirectory.Client} recorded, and is shown by its address.
   *
   * <p>The client with id {@code 7} has never sent a {@code HELLO}. The directory must report that
   * it is not named, return an empty {@link Optional} from {@link ClientDirectory#get(long)}, and
   * return {@code CH007} from {@link ClientDirectory#nameOf(long)}.
   */
  @Test
  void unnamedClientsAreShownByAddress() {
    ClientDirectory directory = new ClientDirectory();
    assertFalse(directory.isNamed(7));
    assertEquals(Optional.empty(), directory.get(7));
    assertEquals("CH007", directory.nameOf(7));
  }
}
