package edu.ntnu.bidata.group1.network.server;

import edu.ntnu.bidata.group1.utility.UniversallyAvailableInformation;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
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
  private ClientHandlerList handlers;
  private ExecutorService executor;

  private ServerLogic logic;

  private long nextAvailableId;

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
    this.executor = Executors.newVirtualThreadPerTaskExecutor();

    this.handlers = ClientHandlerList.getInstance();
    this.logic = new ServerLogic(this);
    this.nextAvailableId = 1L;
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
  public void start(CountDownLatch serverReady) throws IOException  {
    serverReady.countDown();

    while (this.server != null && !this.server.isClosed()) {
      try {
        Socket socket = this.server.accept();

        ClientHandler handler = new ClientHandler(socket, useNextAvailableId(), this);

        this.handlers.add(handler);
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
   * Returns the next available ID.
   * After returning, the next available ID is incremented by 1.
   *
   * @return the next available ID.
   */
  private long useNextAvailableId() {
    long nextId = this.nextAvailableId;
    this.nextAvailableId++;
    return nextId;
  }

  /**
   * Returns the next available ID without updating it.
   *
   * @return the next available ID.
   */
  public long peekNextAvailableId() {
    return this.nextAvailableId;
  }

  /**
   * Closes the {@link ServerSocket} and shuts down the {@link ExecutorService}.
   *
   * @throws IOException if an I/O error occurs when closing the socket.
   */
  public void closeServer() throws IOException {
    if (this.server != null && !this.server.isClosed()) {
      this.server.close();
    }
    if (this.executor != null && !this.executor.isShutdown()) {
      this.executor.shutdown();
    }
  }

  /**
   * Returns the logic class that handles the server's logic.
   *
   * @return the {@link ServerLogic} class.
   */
  public ServerLogic logic() {
    return this.logic;
  }

  /**
   * Returns the {@link ClientHandlerList} that contains all connected clients.
   *
   * @return the {@link ClientHandlerList} that contains all connected clients.
   */
  public ClientHandlerList handlers() {
    return this.handlers;
  }

  /**
   * Receives a message and a sender object, and passes them on to the {@link ServerLogic} class.
   *
   * @param message is the message received.
   * @param sender is the sender object.
   * @throws IllegalArgumentException if {@code message} is {@code null}.
   * @throws IllegalArgumentException if {@code sender} is {@code null}.
   */
  @Override
  public void onMessage(String message, ClientHandler sender) {
    if (message == null) {
      throw new IllegalArgumentException("Message cannot be null.");
    }
    if (sender == null) {
      throw new IllegalArgumentException("Sender cannot be null.");
    }

    this.logic.receiveMessage(message, sender);
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
