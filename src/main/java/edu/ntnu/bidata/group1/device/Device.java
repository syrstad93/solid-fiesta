package edu.ntnu.bidata.group1.device;

import edu.ntnu.bidata.group1.network.ClientConnectionHolder;
import edu.ntnu.bidata.group1.network.server.Messenger;
import edu.ntnu.bidata.group1.utility.DeviceType;
import edu.ntnu.bidata.group1.utility.MessageWorker;
import edu.ntnu.bidata.group1.utility.Protocol;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base class for devices that communicate with a server through a client connection.
 *
 * <p>A device has an identifier, a {@link DeviceType} that describes its role, and a
 * {@link ClientConnectionHolder} that manages its connection. Subclasses, such as actuators,
 * sensors, and controllers, provide the behavior for handling incoming messages.
 *
 * <h2>Lifecycle</h2>
 * Creating a device does not connect it. The constructor only sets up the device: its
 * properties and, for an actuator, its commands. {@link #start()} then connects to the server,
 * introduces the device with {@code HELLO}, and calls {@link #onStarted()}, where the device
 * sends its state and, for an actuator, its commands, or a sensor starts measuring:
 * <pre>{@code
 * Light light = new Light("kitchen-light");
 * light.start();
 * }</pre>
 * Because nothing can arrive from the server before {@code start()}, subclasses can initialise
 * their fields normally, and {@code start()} sees a fully constructed object.
 *
 * <h2>State</h2>
 * A device's state is its {@link #properties}, such as {@code powerOn} or {@code temperature}.
 * The device sends it to the server, never straight to other clients: one
 * {@code STATE:<property>,<value>} message per property. The server keeps the latest values and
 * sends them on to every client that has subscribed to the device. A device sends
 * <ul>
 *   <li>its whole state with {@link #publishState()} once it is ready, and again after each
 *       reconnect, and</li>
 *   <li>a single property with {@link #publish(String)} whenever it changes.</li>
 * </ul>
 */
public abstract class Device implements Messenger<String, ClientConnectionHolder> {
  /** The connection to the server; {@code null} until {@link #start()} has been called. */
  private volatile ClientConnectionHolder<Device> connection;
  private volatile long id;
  private final String name;
  private volatile boolean connectedBefore;
  private volatile boolean replaced;
  /**
   * The device's state, sent to the server as {@code STATE:<property>,<value>} messages. Always
   * contains {@code type}. Subclasses add their own, such as {@code powerOn}.
   */
  protected final Map<String, Object> properties;

  /**
   * Creates a device of the given type and name. The device is not connected until
   * {@link #start()} is called.
   *
   * @param name the device's name, for example {@code kitchen-light}; see
   *     {@link #checkName(String)}
   * @param type the role of this device, such as actuator, sensor, or controller
   * @throws IllegalArgumentException if the name is not allowed or the type is null
   */
  protected Device(String name, DeviceType type) {
    checkName(name);
    if (type == null) {
      throw new IllegalArgumentException("Device type cannot be null.");
    }
    this.name = name;
    this.properties = new ConcurrentHashMap<>();
    this.properties.put("type", type);
  }

  /**
   * Connects the device to the server and starts it. Call this once, after creating the device.
   *
   * <p>The device introduces itself with {@code HELLO} as soon as it is connected, and after
   * every reconnect; see {@link #helloValue()}. The server answers with {@code WELCOME:<id>}.
   * Because the name stays the same, a device that reconnects gets the same id back. Once
   * connected, {@link #onStarted()} runs.
   *
   * @throws IllegalStateException if the device has already been started
   * @throws IOException if the server cannot be reached after all retries
   * @throws InterruptedException if the thread is interrupted while connecting
   */
  public final synchronized void start() throws IOException, InterruptedException {
    if (connection != null) {
      throw new IllegalStateException(name + " has already been started.");
    }
    connection = new ClientConnectionHolder<>(this, this::onConnected);
    onStarted();
  }

  /**
   * Returns whether {@link #start()} has been called successfully.
   *
   * @return {@code true} once the device has been connected the first time
   */
  public boolean isStarted() {
    return connection != null;
  }

  /**
   * Checks that {@link #start()} has been called.
   *
   * @throws IllegalStateException if the device has not been started
   */
  protected final void requireStarted() {
    if (connection == null) {
      throw new IllegalStateException(name + " is not connected; call start() first.");
    }
  }

  /**
   * Runs once, at the end of {@link #start()}, when the device is connected and has introduced
   * itself. Sends the whole state to the server. Subclasses that override this, for example to
   * announce commands or start measuring, should call {@code super.onStarted()}.
   */
  protected void onStarted() {
    publishState();
  }

  /**
   * Returns the value of the {@code HELLO} message this device introduces itself with:
   * {@code <name>,<type>}. A controller adds its role.
   *
   * @return the value, for example {@code kitchen-light,ACTUATOR}
   */
  protected String helloValue() {
    return name + "," + getType();
  }

  /**
   * Checks that a name can be used for a device. A name must not be empty, and must not contain
   * {@code / = : ,} or line breaks, because those are used to separate the parts of a message.
   *
   * @param name the name to check
   * @throws IllegalArgumentException if the name cannot be used
   */
  public static void checkName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Device name cannot be empty.");
    }
    for (char c : "/=:,\n\r".toCharArray()) {
      if (name.indexOf(c) >= 0) {
        throw new IllegalArgumentException("Device name cannot contain '" + c + "': " + name);
      }
    }
  }

  /**
   * Returns a name made of the given prefix and a random part, for example
   * {@code light-3f9a1c2e}. Used by devices created without a name. Such a name stays the same
   * while the program runs, so the device keeps its id across reconnects, but not across
   * restarts of the program.
   *
   * @param prefix the start of the name, for example {@code light}
   * @return the name
   */
  protected static String randomName(String prefix) {
    return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
  }

  /**
   * Runs after every successful connection to the server, first and later ones: introduces this
   * device with {@code HELLO}, and after a reconnect calls {@link #onReconnected()}.
   *
   * @param holder the connection that was just made; used directly, because on the first
   *     connection {@link #connection} is not assigned yet
   */
  private void onConnected(ClientConnectionHolder<Device> holder) {
    MessageWorker message = new MessageWorker()
        .setDestination(MessageWorker.SERVER)
        .setSource("CH", this.id)
        .setCommand(Protocol.HELLO.withValue(helloValue()))
        .buildMessage();
    try {
      holder.message(message.getMessage());
    } catch (IOException e) {
      System.out.println("Could not introduce " + name + " to the server: " + e.getMessage());
      return;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return;
    }
    if (connectedBefore) {
      onReconnected();
    }
    connectedBefore = true;
  }

  /**
   * Runs after this device has reconnected and introduced itself again. Sends the whole state,
   * since the server forgets a device's state when its connection is lost. Subclasses that
   * override this should call {@code super.onReconnected()}.
   */
  protected void onReconnected() {
    publishState();
  }

  /**
   * Sets this device's identifier, as given by the server in {@code WELCOME:<id>}.
   *
   * @param id the identifier
   */
  protected void setId(long id) {
    this.id = id;
  }

  /**
   * Returns this device's name, as given to the constructor.
   *
   * @return the name, for example {@code kitchen-light}
   */
  public String getName() {
    return name;
  }

  /**
   * Returns this device's identifier, the client id the server gave it.
   *
   * @return the identifier, or 0 before the server has answered {@code HELLO}
   */
  protected long getId() {
    return id;
  }

  /**
   * Returns this device's type.
   *
   * @return the type given to the constructor
   */
  protected DeviceType getType() {
    return ((DeviceType) this.properties.get("type"));
  }

  /**
   * Handles a lost connection by connecting to the server again. Once connected, the device
   * introduces itself again and calls {@link #onReconnected()}.
   *
   * <p>If the server cannot be reached after all retries, the device stays disconnected and a
   * message is printed.
   *
   * @param client the client connection holder that was disconnected
   */
  @Override
  public void onDisconnect(ClientConnectionHolder client) {
    if (replaced) {
      System.out.println(name + " was replaced by a newer connection with the same name; "
          + "not reconnecting.");
      return;
    }
    try {
      client.retryConnection();
    } catch (IOException e) {
      System.out.println("Could not reconnect to the server: " + e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Sends a single message from this device to the given destination.
   *
   * <p>The command must not contain {@code /}, because that character separates the parts of a
   * message. Sending errors are printed instead of thrown, so callers on listener threads are
   * not interrupted by a failed send.
   *
   * @param destination the destination, for example {@code CH012}, {@code SE000} or
   *     {@code AL000}
   * @param command the command part of the message, usually {@code key:value}
   * @throws IllegalStateException if the device has not been started
   */
  protected void sendMessage(String destination, String command) {
    requireStarted();
    ClientConnectionHolder<Device> current = connection;
    MessageWorker msg = new MessageWorker()
        .setDestination(destination)
        .setSource("CH", this.getId())
        .setCommand(command)
        .buildMessage();
    try {
      current.message(msg.getMessage());
    } catch (IOException e) {
      System.out.println("Could not send message to " + destination + ": " + e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  /**
   * Handles the protocol messages every device understands, so subclasses do not have to.
   *
   * <ul>
   *   <li>{@link Protocol#WELCOME} from the server: sets this device's id.</li>
   *   <li>{@link Protocol#PING}: answers {@code PONG}, so the server knows the device is still
   *       there.</li>
   *   <li>{@link Protocol#REPLACED} from the server: another client connected with this
   *       device's name, so this device will not reconnect when the server closes its
   *       connection.</li>
   * </ul>
   *
   * @param protocol the protocol message received
   * @param argument the message's value, for example the id for {@code WELCOME}
   * @param replyTo the sender's address
   * @return {@code true} if the message was handled here, {@code false} if the caller should
   *     handle it
   */
  protected boolean handleCommonRequest(Protocol protocol, String argument, String replyTo) {
    switch (protocol) {
      case WELCOME -> {
        if (replyTo != null && replyTo.startsWith("SE")) {
          try {
            setId(Long.parseLong(argument));
          } catch (NumberFormatException e) {
            // Malformed id; keep the current one.
          }
        }
        return true;
      }
      case PING -> {
        sendMessage(replyTo, Protocol.PONG.getKey());
        return true;
      }
      case REPLACED -> {
        if (replyTo != null && replyTo.startsWith("SE")) {
          replaced = true;
        }
        return true;
      }
      default -> {
        return false;
      }
    }
  }

  /**
   * Tells the given destination that one of its messages was rejected, with
   * {@code ERROR:<request>,<reason>}.
   *
   * @param destination the sender of the rejected message
   * @param requestKey the key of the rejected message, for example {@code value}
   * @param reason why it was rejected
   */
  protected void sendError(String destination, String requestKey, String reason) {
    sendMessage(destination, Protocol.error(requestKey, reason));
  }

  /**
   * Sends every property to the server, one {@code STATE:<property>,<value>} message each.
   */
  protected void publishState() {
    for (String property : properties.keySet()) {
      publish(property);
    }
  }

  /**
   * Sends one property to the server as {@code STATE:<property>,<value>}. Call this after
   * changing a property.
   *
   * <p>Before {@link #start()}, nothing is sent: the whole state is sent when the device
   * starts.
   *
   * @param property the property's name; nothing is sent if the device has no such property
   */
  protected void publish(String property) {
    Object value = properties.get(property);
    if (value != null && isStarted()) {
      sendMessage(MessageWorker.SERVER, Protocol.STATE.withValue(property + "," + value));
    }
  }

  /**
   * Sends every property whose value differs from the given earlier copy of the properties.
   *
   * @param before a copy of {@link #properties} taken before the change
   */
  protected void publishChanges(Map<String, Object> before) {
    for (Map.Entry<String, Object> entry : properties.entrySet()) {
      if (!entry.getValue().equals(before.get(entry.getKey()))) {
        publish(entry.getKey());
      }
    }
  }
}
