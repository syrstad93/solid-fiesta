package edu.ntnu.bidata.group1.network;

import edu.ntnu.bidata.group1.network.server.Messenger;
import edu.ntnu.bidata.group1.utility.UniversallyAvailableInformation;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * A TCP client that connects to a server and exchanges UTF-encoded text messages with it.
 *
 * <p>When constructed, the client connects to the server at the address
 * {@link UniversallyAvailableInformation#LOCALHOST} and the port
 * {@link UniversallyAvailableInformation#PORT_NUMBER}. If a connection attempt fails, the
 * client retries up to three times, waiting two seconds between attempts.
 *
 * <p>Messages sent by the server are received on a background daemon thread and printed to
 * the standard output. Text is sent to the server with {@link #message(String)}. Once the
 * client is no longer needed, {@link #close()} should be called to release the socket.
 *
 * @param <ClientType> is the type of the object for whom this class will hold a connection.
 *                    The class that uses a {@link ClientConnectionHolder} must implement
 *                    the {@link Messenger} interface.
 */
public class ClientConnectionHolder<ClientType extends Messenger<String, ClientConnectionHolder>> {

  private Socket socket;
  private DataOutputStream out;
  private DataInputStream in;
  private Boolean connected = false;
  private int retries = 0;
  private final String address;
  private final int port;

  private ClientType client;

  /**
   * Creates a new client and connects it to the server.
   *
   * <p>The client address is set to the local host and the port is set to the shared port number
   * defined in {@link UniversallyAvailableInformation}. The connection is then established through
   * {@link #connect()}.
   *
   * @param client is the client for whom this class holds a connection.
   * @throws IOException if an I/O error occurs while connecting to the server.
   * @throws InterruptedException if the thread is interrupted while waiting between connection
   *     attempts.
   */
  public ClientConnectionHolder(ClientType client) throws IOException, InterruptedException {
    setClient(client);
    this.address = UniversallyAvailableInformation.LOCALHOST;
    this.port = UniversallyAvailableInformation.PORT_NUMBER;
    connect();
  }

  /**
   * Establishes a socket connection to the server and starts listening for incoming messages.
   *
   * <p>A new {@link Socket} is opened to the configured address and port, and the input and
   * output streams are wrapped in {@link DataInputStream} and {@link DataOutputStream}
   * respectively. On success, the client is marked as connected, the retry counter is reset to
   * zero, and the background listener is started through {@link #receive()}.
   *
   * <p>If the connection attempt fails with an {@link IOException}, {@link #retryConnection()} is
   * invoked to schedule another attempt. The original exception is then wrapped in a
   * {@link RuntimeException} and thrown.
   *
   * @throws IOException if an I/O error occurs while retrying the connection.
   * @throws InterruptedException if the thread is interrupted while waiting between retry
   *     attempts.
   * @throws RuntimeException if the initial connection attempt fails, wrapping the original
   *     {@link IOException}.
   */
  private void connect() throws IOException, InterruptedException {
    try {
      socket = new Socket(address, port);
      out = new DataOutputStream(socket.getOutputStream());
      in = new DataInputStream(socket.getInputStream());
      connected = true;
      retries = 0;
      receive();
    } catch (IOException e) {
      retryConnection();
      throw new RuntimeException(e);
    }
  }

  /**
   * Sends a UTF-encoded message to the server through the output stream.
   *
   * <p>The message is written and the output stream is flushed so that it is delivered
   * immediately. If an {@link IOException} occurs while writing, {@link #retryConnection()} is
   * invoked to attempt to re-establish the connection to the server.
   *
   * @param message the text message to send to the server.
   * @throws IOException if an I/O error occurs while sending the message and the retry attempt
   *     also fails.
   * @throws InterruptedException if the thread is interrupted while waiting between retry
   *     attempts.
   */
  public void message(String message) throws IOException, InterruptedException {
    try {
      out.writeUTF(message);
      out.flush();
    } catch (IOException io) {
      retryConnection();
    }
  }

  /**
   * Starts a daemon thread that listens for messages sent by the server.
   *
   * <p>The listener thread runs while the client is connected, reading UTF-encoded messages from
   * the input stream and printing each one prefixed with the server tag. If an
   * {@link IOException} occurs while reading and the client is still marked as connected, a
   * message describing the lost connection is printed. The thread is marked as a daemon so that
   * it does not prevent the application from exiting.
   */
  private void receive() {
    Thread listener = new Thread(() -> {
      try {
        while (connected) {
          this.client.onMessage(in.readUTF(), this);
        }
      } catch (IOException e) {
        if (connected) {
          System.out.println("Connection lost: " + e.getMessage());
        }
      }
    });
    listener.setDaemon(true);
    listener.start();
  }

  /**
   * Attempts to re-establish the connection to the server after a failed attempt.
   *
   * <p>The retry counter is incremented on each call. While the counter is below the maximum of
   * three retries, a message is printed, the current thread waits for two seconds, and
   * {@link #connect()} is invoked again. Once the limit is reached, no further attempts are made
   * and the method returns without taking any action.
   *
   * @throws IOException if an I/O error occurs while reconnecting to the server.
   * @throws InterruptedException if the thread is interrupted while waiting between retry
   *     attempts.
   */
  private void retryConnection() throws IOException, InterruptedException {
    if (retries++ < 3) {
      System.out.println("Retrying connection...");
      Thread.sleep(2000);
      connect();
    }
  }

  /**
   * Closes the client connection to the server.
   *
   * <p>The client is marked as disconnected before the underlying {@link Socket} is closed. If an
   * {@link IOException} occurs while closing the socket, the error message is printed to the
   * standard output and the method returns without rethrowing the exception.
   */
  public void close() {
    connected = false;
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
  private void setClient(ClientType client) {
    if (client == null) {
      throw new IllegalArgumentException("ClientConnectionHolder cannot be null");
    }
    this.client = client;
  }
}
