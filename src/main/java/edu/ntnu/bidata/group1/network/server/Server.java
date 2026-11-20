package edu.ntnu.bidata.group1.network.server;

import edu.ntnu.bidata.group1.utility.UniversallyAvailableInformation;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Handles server functionalities.
 *
 * <ul>
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
 * </ul>
 */
public class Server implements Messenger<String, ClientHandler> {

  /** How often the server sends {@code PING} to every client, in milliseconds. */
  public static final long HEARTBEAT_INTERVAL_MS = 5000;

  /** How long a client may be silent before the server drops it, in milliseconds. */
  public static final long CLIENT_TIMEOUT_MS = 15000;

  private static volatile Server instance;

  private ServerSocket server;
  private ClientHandlerList handlers;
  private ExecutorService executor;
  private ScheduledExecutorService heartbeat;
  private volatile Thread acceptThread;

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
    this.server = new ServerSocket();
    this.server.setReuseAddress(true);
    this.server.bind(new InetSocketAddress(UniversallyAvailableInformation.PORT_NUMBER));
    this.executor = Executors.newVirtualThreadPerTaskExecutor();

    this.handlers = ClientHandlerList.getInstance();
    this.logic = new ServerLogic(this);
    this.nextAvailableId = this.handlers.size() + 1;
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
   * <p>Accepts new socket connections, and adds them to the {@link ExecutorService}. Also starts
   * the heartbeat, see {@link #HEARTBEAT_INTERVAL_MS}.
   *
   * <p>The server is kept running by a loop that continues as long as
   * the class field {@link #server} is not {@code null} and
   * the {@link ServerSocket} that it refers to remains open.
   *
   * @param serverReady is a {@link CountDownLatch} that is counted down when the server is ready
   *                    to accept connections.
   * @throws IOException if an {@link IOException} is thrown by the {@link ServerSocket}
   *        that is not caused by it purposely being closed down.
   * @throws IllegalArgumentException if {@code serverReady} is {@code null}.
   */
  public void start(CountDownLatch serverReady) throws IOException {
    if (serverReady == null) {
      throw new IllegalArgumentException("CountDownLatch cannot be null.");
    }

    this.acceptThread = Thread.currentThread();
    startHeartbeat();
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
   * Starts sending {@code PING} to every client every {@value #HEARTBEAT_INTERVAL_MS}
   * milliseconds, and closing the connection to any client that has sent nothing for
   * {@value #CLIENT_TIMEOUT_MS} milliseconds. A closed connection is cleaned up the same way as
   * one the client closed, through {@link #onDisconnect(ClientHandler)}.
   *
   * <p>This finds clients whose connection died without either side noticing, for example
   * because the network cable was pulled.
   */
  private void startHeartbeat() {
    if (this.heartbeat != null) {
      return;
    }
    this.heartbeat = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable, "server-heartbeat");
      thread.setDaemon(true);
      return thread;
    });
    this.heartbeat.scheduleAtFixedRate(() -> {
      try {
        this.logic.heartbeat(CLIENT_TIMEOUT_MS);
      } catch (RuntimeException e) {
        System.out.println("Heartbeat failed: " + e.getMessage());
      }
    }, HEARTBEAT_INTERVAL_MS, HEARTBEAT_INTERVAL_MS, TimeUnit.MILLISECONDS);
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
   * After closing the server, the {@link #instance} is set to {@code null},
   * which allows for an entirely new instance to be created.
   *
   * <p>Waits up to one second for the thread running {@link #start(CountDownLatch)} to stop.
   * While that thread is blocked waiting for a connection, Java only releases the port once the
   * thread has woken up, so a new server opened right away could otherwise fail with
   * "Address already in use".
   *
   * @throws IOException if an I/O error occurs when closing the socket.
   */
  public void closeServer() throws IOException {
    if (this.server != null && !this.server.isClosed()) {
      this.server.close();
    }
    Thread accepting = this.acceptThread;
    if (accepting != null && accepting != Thread.currentThread()) {
      try {
        accepting.join(1000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
    if (this.executor != null && !this.executor.isShutdown()) {
      this.executor.shutdown();
    }
    if (this.heartbeat != null) {
      this.heartbeat.shutdownNow();
    }

    instance = null;
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
   * Receives a client that disconnects, removes it from the {@link ClientHandlerList}, and lets
   * {@link ServerLogic#onClientDisconnected(ClientHandler)} clean up after it.
   *
   * <p>Nothing is cleaned up if the connection had already been replaced by a newer one from the
   * same client, which then has the same id.
   *
   * @param client is the client that disconnects.
   * @throws IllegalArgumentException if {@code client} is {@code null}.
   */
  @Override
  public void onDisconnect(ClientHandler client) {
    if (client == null) {
      throw new IllegalArgumentException("Client cannot be null.");
    }

    synchronized (this.handlers) {
      if (this.handlers.removeIfCurrent(client)) {
        this.logic.onClientDisconnected(client);
      }
    }
  }
}
