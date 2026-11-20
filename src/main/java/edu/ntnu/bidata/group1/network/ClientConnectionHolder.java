package edu.ntnu.bidata.group1.network;

import edu.ntnu.bidata.group1.network.server.Messenger;
import edu.ntnu.bidata.group1.utility.UniversallyAvailableInformation;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * A TCP client that connects to a server and exchanges text messages with it, one message per
 * line.
 *
 * <p>The line-based format matches the server's {@code ClientHandler}, which reads with
 * {@code readLine()} and writes with {@code println()}. Messages therefore must not contain line
 * breaks.
 *
 * <p>When constructed, the client connects to the server at the address
 * {@link UniversallyAvailableInformation#LOCALHOST} and the port
 * {@link UniversallyAvailableInformation#PORT_NUMBER}. If a connection attempt fails, the
 * client retries up to {@value #MAX_RETRIES} times, waiting {@value #RETRY_DELAY_MS} milliseconds
 * between attempts.
 *
 * <p>The connection tells its client what happens through the {@link Messenger} interface:
 * <ul>
 *   <li>{@link Messenger#onConnect(Object)} after every successful connection, including
 *       reconnections, before anything else is sent. A device uses it to introduce itself with
 *       {@code HELLO}. It runs while this object is locked, so no other thread can send anything
 *       before it is done.</li>
 *   <li>{@link Messenger#onMessage(Object, Object)} for every message from the server, on a
 *       background daemon thread.</li>
 *   <li>{@link Messenger#onDisconnect(Object)} when the connection is lost, which for a
 *       {@code Device} reconnects to the server.</li>
 * </ul>
 *
 * <p>The server sends {@code PING} every few seconds. If nothing at all arrives from the server
 * for {@value #SERVER_TIMEOUT_MS} milliseconds, the connection is treated as lost: it is closed,
 * and {@code onDisconnect} reconnects. This finds a connection that died without either side
 * noticing.
 *
 * @param <ClientT> is the type of the object for whom this class will hold a connection.
 *                    The class that uses a {@link ClientConnectionHolder} must implement
 *                    the {@link Messenger} interface.
 */
public class ClientConnectionHolder<ClientT extends Messenger<String, ClientConnectionHolder>> {

  private static final int MAX_RETRIES = 3;
  private static final long RETRY_DELAY_MS = 2000;

  /** How long the server may be silent before the connection is treated as lost, in ms. */
  public static final long SERVER_TIMEOUT_MS = 15000;

  private volatile Socket socket;
  private PrintWriter out;
  private BufferedReader in;
  private volatile boolean connected = false;
  private Thread listener;
  private Socket listenerSocket;
  private final String address;
  private final int port;

  private ClientT client;
  private volatile long lastReceived = System.currentTimeMillis();

  /**
   * Creates a new client and connects it to the server. The client's
   * {@link Messenger#onConnect(Object)} runs once the connection is made, before this
   * constructor returns.
   *
   * @param client is the client for whom this class holds a connection.
   * @throws IOException if the server cannot be reached after all retries.
   * @throws InterruptedException if the thread is interrupted while waiting between connection
   *     attempts.
   */
  public ClientConnectionHolder(ClientT client) throws IOException, InterruptedException {
    setClient(client);
    this.address = UniversallyAvailableInformation.LOCALHOST;
    this.port = UniversallyAvailableInformation.PORT_NUMBER;
    connect();
    startWatchdog();
  }

  /**
   * Opens a socket to the server and starts listening for incoming messages.
   *
   * <p>If an attempt fails, it is retried up to {@value #MAX_RETRIES} times with a delay of
   * {@value #RETRY_DELAY_MS} milliseconds. The method returns normally as soon as one attempt
   * succeeds.
   *
   * @throws IOException if every attempt fails; this is the error from the last attempt.
   * @throws InterruptedException if the thread is interrupted while waiting between attempts.
   */
  private synchronized void connect() throws IOException, InterruptedException {
    int attempt = 0;
    while (true) {
      try {
        socket = new Socket(address, port);
        out = new PrintWriter(
            new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        in = new BufferedReader(
            new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        connected = true;
        lastReceived = System.currentTimeMillis();
        receive();
        this.client.onConnect(this);
        return;
      } catch (IOException e) {
        connected = false;
        if (attempt++ >= MAX_RETRIES) {
          throw e;
        }
        System.out.println("Could not connect (" + e.getMessage() + "). Retrying ("
            + attempt + "/" + MAX_RETRIES + ")...");
        Thread.sleep(RETRY_DELAY_MS);
      }
    }
  }

  /**
   * Sends a message to the server as a single line.
   *
   * <p>If sending fails, the connection is re-established once and the message is sent again.
   * The method is synchronized so that messages sent from different threads are never mixed
   * together.
   *
   * @param message the text message to send to the server; must not contain line breaks.
   * @throws IOException if the message cannot be sent, even after reconnecting.
   * @throws InterruptedException if the thread is interrupted while reconnecting.
   * @throws IllegalArgumentException if {@code message} is null or contains a line break.
   */
  public synchronized void message(String message) throws IOException, InterruptedException {
    if (message == null || message.indexOf('\n') >= 0 || message.indexOf('\r') >= 0) {
      throw new IllegalArgumentException("Message cannot be null or contain line breaks.");
    }
    if (connected) {
      out.println(message);
      if (!out.checkError()) {
        return;
      }
    }
    retryConnection();
    out.println(message);
    if (out.checkError()) {
      throw new IOException("Could not send message to the server.");
    }
  }

  /**
   * Starts a daemon thread that listens for messages sent by the server.
   *
   * <p>Each received line is passed to the client's {@code onMessage}. When the server closes
   * the connection or reading fails, the client's {@code onDisconnect} is called. It is not
   * called when the connection is replaced by {@link #retryConnection()}.
   *
   * <p>Only one listener reads from a connection. Calling this while a listener is already
   * reading from the current connection has no effect, so two threads never read from the same
   * connection.
   */
  public synchronized void receive() {
    // A listener is only reused if it is still reading from the current socket. After a
    // reconnect, the old listener may still be alive for a moment, reading from the old socket,
    // so a new one must be started for the new socket.
    if (listener != null && listener.isAlive() && listener != Thread.currentThread()
        && listenerSocket == this.socket) {
      return;
    }
    BufferedReader reader = this.in;
    Socket listenedSocket = this.socket;
    listenerSocket = listenedSocket;
    listener = new Thread(() -> {
      try {
        String line;
        while (connected && (line = reader.readLine()) != null) {
          lastReceived = System.currentTimeMillis();
          try {
            this.client.onMessage(line, this);
          } catch (RuntimeException e) {
            System.out.println("Error while handling message \"" + line + "\": " + e);
          }
        }
      } catch (IOException e) {
        if (connected && this.socket == listenedSocket) {
          System.out.println("Connection lost: " + e.getMessage());
        }
      }
      // Only report a disconnect for the connection this thread was listening to. After a
      // reconnect, the old listener ends too, but the new connection is fine.
      if (connected && this.socket == listenedSocket) {
        connected = false;
        this.client.onDisconnect(this);
      }
    });
    listener.setDaemon(true);
    listener.start();
  }

  /**
   * Starts a background thread that closes the connection if nothing has arrived from the server
   * for {@value #SERVER_TIMEOUT_MS} milliseconds. Closing it makes the listener report the lost
   * connection, so the client's {@code onDisconnect} reconnects.
   */
  private void startWatchdog() {
    Thread watchdog = new Thread(() -> {
      while (!Thread.currentThread().isInterrupted()) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          return;
        }
        if (connected && System.currentTimeMillis() - lastReceived > SERVER_TIMEOUT_MS) {
          System.out.println("Nothing from the server for " + SERVER_TIMEOUT_MS / 1000
              + " s; treating the connection as lost.");
          lastReceived = System.currentTimeMillis();
          closeSocket();
        }
      }
    });
    watchdog.setDaemon(true);
    watchdog.start();
  }

  /**
   * Closes the current connection, if any, and connects to the server again.
   *
   * @throws IOException if the server cannot be reached after all retries.
   * @throws InterruptedException if the thread is interrupted while waiting between attempts.
   */
  public synchronized void retryConnection() throws IOException, InterruptedException {
    System.out.println("Reconnecting to the server...");
    connected = false;
    closeSocket();
    connect();
  }

  private void closeSocket() {
    if (socket == null) {
      return;
    }
    try {
      socket.close();
    } catch (IOException io) {
      System.out.println(io.getMessage());
    }
  }

  /**
   * Sets the client to whom this class holds a connection for.
   *
   * @param client is the client.
   * @throws IllegalArgumentException if {@code client} is {@code null}.
   */
  private void setClient(ClientT client) {
    if (client == null) {
      throw new IllegalArgumentException("Client cannot be null.");
    }
    this.client = client;
  }
}
