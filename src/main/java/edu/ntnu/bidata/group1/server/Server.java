package edu.ntnu.bidata.group1.server;

import edu.ntnu.bidata.group1.utility.UniversallyAvailableInformation;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles server functionalities.
 *
 * <p>
 *   Has a {@link ServerSocket} that accepts connection requests from external sockets.
 *   On acceptance, a {@link Socket} is created and passed to a new {@link ClientHandler}
 *   that maintains the connection.
 * </p>
 * <p>
 *   Has a {@link Set} containing all {@link ClientHandler} objects.
 *   The set is a {@link ConcurrentHashMap} to ensure thread safety.
 * </p>
 * <p>
 *   Has an {@link ExecutorService} that manages the execution of tasks using threads.
 *   The number of threads is unbounded.
 * </p>
 */
public class Server implements Messenger<String, ClientHandler> {

  private static volatile Server instance;

  private ServerSocket server;
  private Set<ClientHandler> handlers;
  private ExecutorService executor;

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
    this.handlers = ConcurrentHashMap.newKeySet();
    this.executor = Executors.newVirtualThreadPerTaskExecutor();
  }

  /**
   * Returns the {@link Server} set to {@link #instance},
   * or creates a new instance if one doesn't exist.
   *
   * @return the {@link Server} in {@link #instance}.
   * @throws IOException if instantiating a new {@link Server} and an I/O error occurs when
   *        opening the socket when setting the {@link ServerSocket}.
   */
  public static Server getInstance() throws IOException {
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
   * <p>
   *   The server runs as long as {@link #server} is not {@code null} and remains open.
   * </p>
   * <p>
   *   Accepts new socket connections, and adds them to the {@link ExecutorService}.
   * </p>
   *
   * @throws IOException if an {@link IOException} is thrown by the {@link ServerSocket}
   *        that is not caused by it closing down.
   */
  public void start() throws IOException  {
    while (this.server != null && !this.server.isClosed()) {
      try {
        Socket socket = this.server.accept();
        ClientHandler handler = new ClientHandler(socket, this);

        this.handlers.add(handler);
        this.executor.submit(handler);

      } catch (IOException e) {
        if (this.server.isClosed()) {
          // Do nothing. The loop will end.
        }
      }
    }
  }

  @Override
  public void onMessage(String message, ClientHandler sender) {
    // TODO: Implement what to do when receiving messages.
  }

  @Override
  public void onDisconnect(ClientHandler client) {
    // TODO: Implement what to do when receiving notice that a socket is disconnecting.
  }
}
