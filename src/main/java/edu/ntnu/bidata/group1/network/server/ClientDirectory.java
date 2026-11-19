package edu.ntnu.bidata.group1.network.server;

import edu.ntnu.bidata.group1.utility.DeviceType;
import edu.ntnu.bidata.group1.utility.MessageWorker;
import edu.ntnu.bidata.group1.utility.Role;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Remembers every client that has introduced itself with {@code HELLO}: its name, its type and,
 * for controllers, its role, and which id belongs to which name.
 *
 * <p>Names are what make ids stable. A client that connects again with a name it has used before
 * gets the same id back, so its registration, rooms, automations and subscribers still apply.
 * Entries are kept after a client disconnects, for exactly that reason.
 *
 * <p>This class only keeps track; it does not send messages. All methods are synchronized.
 */
public class ClientDirectory {

  /**
   * What a client said about itself in its {@code HELLO}.
   *
   * @param name the client's name, for example {@code kitchen-light}
   * @param type what kind of client it is
   * @param role the controller's role; {@code null} for every other type
   */
  public record Client(String name, DeviceType type, Role role) {
  }

  private final Map<String, Long> idsByName = new HashMap<>();
  private final Map<Long, Client> clientsById = new HashMap<>();

  /**
   * Creates an empty directory.
   */
  public ClientDirectory() {
    // Nothing to set up.
  }

  /**
   * Reads the value of a {@code HELLO} message.
   *
   * <p>The type must be one of the {@link DeviceType} constants and the role one of the
   * {@link Role} constants, written exactly as in the enums, for example {@code ACTUATOR} or
   * {@code OWNER}. Clients and server share these enums, so they always agree on what is
   * allowed.
   *
   * @param value {@code <name>,<type>} or {@code <name>,CONTROLLER,<role>}
   * @return what the client said about itself
   * @throws IllegalArgumentException if the value is malformed, the type or role is unknown, a
   *     controller gives no role, or another client type gives one
   */
  public static Client parseHello(String value) {
    String[] parts = value == null ? new String[0] : value.split(",", -1);
    if (parts.length < 2 || parts.length > 3) {
      throw new IllegalArgumentException(
          "HELLO needs a name and a type, for example HELLO:kitchen-light,ACTUATOR");
    }
    String name = parts[0].trim();
    String typeName = parts[1].trim();
    String roleName = parts.length == 3 ? parts[2].trim() : "";
    if (name.isEmpty() || name.chars().anyMatch(c -> "/=:,\n\r".indexOf(c) >= 0)) {
      throw new IllegalArgumentException("Client name cannot be empty or contain ':'.");
    }

    DeviceType type;
    try {
      type = DeviceType.valueOf(typeName);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Client type must be one of "
          + Arrays.toString(DeviceType.values()) + ": " + typeName);
    }

    if (type != DeviceType.CONTROLLER) {
      if (!roleName.isEmpty()) {
        throw new IllegalArgumentException("Only a controller gives a role.");
      }
      return new Client(name, type, null);
    }

    Role role;
    try {
      role = Role.valueOf(roleName);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("A controller must give a role, one of "
          + Arrays.toString(Role.values()) + ": " + roleName);
    }
    return new Client(name, type, role);
  }

  /**
   * Records a client's {@code HELLO} and decides its id.
   *
   * @param client what the client said about itself
   * @param currentId the id the client's connection was given when it connected
   * @return the id the client should use: the id this name had before, or {@code currentId} if
   *     the name is new
   */
  public synchronized long introduce(Client client, long currentId) {
    Long known = idsByName.get(client.name());
    long id = known != null ? known : currentId;
    idsByName.put(client.name(), id);
    clientsById.put(id, client);
    return id;
  }

  /**
   * Returns what a client said about itself.
   *
   * @param id the client's id
   * @return the client, or empty if it has not sent {@code HELLO}
   */
  public synchronized Optional<Client> get(long id) {
    return Optional.ofNullable(clientsById.get(id));
  }

  /**
   * Returns whether a client has introduced itself with a name.
   *
   * @param id the client's id
   * @return {@code true} if it has
   */
  public synchronized boolean isNamed(long id) {
    return clientsById.containsKey(id);
  }

  /**
   * Returns a client's name, or its address if it has not introduced itself.
   *
   * @param id the client's id
   * @return for example {@code kitchen-light}, or {@code CH004}
   */
  public synchronized String nameOf(long id) {
    Client client = clientsById.get(id);
    return client != null ? client.name() : MessageWorker.address("CH", id);
  }
}
