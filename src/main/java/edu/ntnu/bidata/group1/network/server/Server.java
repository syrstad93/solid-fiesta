package edu.ntnu.bidata.group1.network.server;

import edu.ntnu.bidata.group1.utility.UniversallyAvailableInformation;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles server functionalities.
 *
 * <li>
 *   Has a {@link ServerSocket} that accepts connection requests from external sockets.
 *   On acceptance, a {@link Socket} is created and passed to a new {@link ClientHandler}
 *   that maintains the connection. The client handler is provided with a unique ID.
 * </li>
 * <li>
 *   Has a {@link Map} containing all {@link ClientHandler} objects.
 *   The map is a {@link ConcurrentHashMap} to ensure thread safety.
 * </li>
 * <li>
 *   Has an {@link ExecutorService} that manages the execution of tasks using threads.
 *   The number of threads is unbounded.
 * </li>
 */
public class Server implements Messenger<String, ClientHandler> {

  private static volatile Server instance;

  private ServerSocket server;
  private Map<Long, ClientHandler> handlers;
  private ExecutorService executor;

  private long lastUsedId;

  /**
   * Instantiates {@link Server} with a ServerSocket that uses the port number given by
   * {@link UniversallyAvailableInformation#PORT_NUMBER}.
   *
   * @throws IOException if an I/O error occurs when opening the socket
   *      when setting the {@link ServerSocket}.
   * @throws IllegalArgumentException if the port number given to the {@link ServerSocket}
   *        is outside the specified range of valid port values,
   *        which is between {@code 0} and {@code 65535}, inclusive.
   */
  private Server() throws IOException, IllegalArgumentException {
    this.server = new ServerSocket(UniversallyAvailableInformation.PORT_NUMBER);
    this.handlers = new ConcurrentHashMap<>();
    this.executor = Executors.newVirtualThreadPerTaskExecutor();
    this.lastUsedId = 1L;
  }

  /**
   * Returns the {@link Server} set to {@link #instance},
   * or creates a new instance if one doesn't exist.
   *
   * @return the {@link Server} in {@link #instance}.
   * @throws IOException if instantiating a new {@link Server}, and an I/O error occurs when
   *        opening the socket when setting the {@link ServerSocket}.
   * @throws IllegalArgumentException if instantiating a new {@link Server}, and the port number
   *        given to the {@link ServerSocket} is outside the specified range of valid port values,
   *        which is between {@code 0} and {@code 65535}, inclusive.
   */
  public static Server getInstance() throws IOException, IllegalArgumentException {
    if (instance == null) {
      synchronized (Server.class) {
        if (instance == null) {
          instance = new Server();
        }
      }
    }
    return instance;
  }

  /**
   * Starts the server and keeps it running.
   *
   *  <p>
   *   Accepts new socket connections, and adds them to the {@link ExecutorService}.
   * </p>
   * <p>
   *   The server is kept running by a loop that continues as long as
   *   the class field {@link #server} is not {@code null} and
   *   the {@link ServerSocket} that it refers to remains open.
   * </p>
   *
   * @throws IOException if an {@link IOException} is thrown by the {@link ServerSocket}
   *        that is not caused by it purposely being closed down.
   */
  public void start() throws IOException  {
    while (this.server != null && !this.server.isClosed()) {
      try {
        Socket socket = this.server.accept();
        long id = getNextAvailableId();

        ClientHandler handler = new ClientHandler(socket, id, this);

        this.handlers.put(id, handler);
        this.executor.submit(handler);

      } catch (IOException e) {
        // If this.server is closed, this exception is expected. Do nothing.
        if (!this.server.isClosed()) {
          throw e;
        }
      }
    }
  }

  /**
   * Increments {@link #lastUsedId} by {@code 1} and returns it.
   *
   * @return the value of {@link #lastUsedId} after incrementing by {@code 1}.
   */
  private long getNextAvailableId() {
    this.lastUsedId++;
    return this.lastUsedId;
  }

  /**
   * Performs whatever action the received message indicates.
   * TODO: Update Javadoc when method is implemented.
   *
   * @param message is the message received.
   * @param sender is the sender object.
   */
  @Override
  public void onMessage(String message, ClientHandler sender) {
    RouteMessage routeMessage = new RouteMessage(message, this.handlers);
    routeMessage.route(message);
  }

  /**
   * Performs whatever actions are necessary to safely disconnect a client.
   * TODO: Update Javadoc when method is implemented.
   *
   * @param client is the client that disconnects.
   */
  @Override
  public void onDisconnect(ClientHandler client) {
    // TODO: Implement what to do when receiving notice that a socket is disconnecting.
  }
}
