package edu.ntnu.bidata.group1.device;

import edu.ntnu.bidata.group1.network.ClientConnectionHolder;
import edu.ntnu.bidata.group1.network.server.Messenger;
import java.io.IOException;

/**
 * Base class for devices that communicate with a server through a client connection.
 *
 * <p>A device has an identifier, a {@link Type} that describes its role, and a
 * {@link ClientConnectionHolder} that manages its connection. Subclasses, such as actuators,
 * sensors, and controllers, provide the behavior for handling incoming messages.
 */
public abstract class Device implements Messenger<String, ClientConnectionHolder> {
  public ClientConnectionHolder connection;
  private long id;
  private final Type type;

  /**
   * Creates a device of the given type and starts receiving messages over its connection.
   *
   * <p>The constructor creates a {@link ClientConnectionHolder} bound to this device, assigns the
   * device type, and begins receiving on the connection.
   *
   * @param type the role of this device, such as actuator, sensor, or controller
   * @throws IOException if the client connection cannot be created or used
   * @throws InterruptedException if the thread is interrupted while receiving
   */
  public Device(Type type) throws IOException, InterruptedException {
    this.connection = new ClientConnectionHolder<>(this);
    this.type = type;
    connection.receive();
  }

  /**
   * Sets the identifier of this device if it has not been assigned yet.
   *
   * <p>The identifier can be assigned only once. If the current identifier is non-zero, the
   * given value is ignored.
   *
   * @param id the identifier to assign to this device
   */
  void setId(long id) {
    if (this.id == 0L) {
      this.id = id;
    }
  }

  long getId() {
    return id;
  }

  Type getType() {
    return type;
  }

  /**
   * Handles a disconnection of the given client connection by attempting to re-establish it.
   *
   * <p>The device's own connection is retried. If the retry fails because of an I/O error or the
   * thread is interrupted, the failure is rethrown as a {@link RuntimeException}.
   *
   * @param client the client connection holder that was disconnected
   * @throws RuntimeException if reconnection fails due to an I/O error or interruption
   */
  @Override
  public void onDisconnect(ClientConnectionHolder client) {
    try {
      connection.retryConnection();
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
