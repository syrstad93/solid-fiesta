package edu.ntnu.bidata.group1.network.server;

import edu.ntnu.bidata.group1.utility.MessageWorker;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Handles a {@link Socket} connection to a remote client.
 * This includes input/output streams and data interchange with the client.
 */
public class ClientHandler implements Runnable  {

  private Socket socket;
  private BufferedReader in;
  private PrintWriter out;

  private Messenger<String, ClientHandler> messenger;

  private long id;
  private String header;
  // TODO: Implement data interchange with client for requesting header information.
  private String lastSentMessage;

  /**
   * Instantiates this class by setting a {@link Socket}
   * that is in communication with a remote client,
   * a {@link BufferedReader} that receives input from the {@link Socket},
   * and a {@link PrintWriter} that sends output to the {@link Socket}.
   *
   * @param socket is the {@link Socket} in communication with a remote client.
   * @param id is a unique ID for this {@link ClientHandler}.
   * @param messenger is an interface where incoming messages are passed on through.
   */
  public ClientHandler(Socket socket, long id, Messenger<String, ClientHandler> messenger)
      throws IOException {
    setSocket(socket);
    setMessenger(messenger);
    setId(id);
    setBufferedReader();
    setPrintWriter();
  }

  /**
   * Maintains a loop that waits to receive command messages from the client.
   * When a message is received, the message source is updated to this {@link ClientHandler},
   * and then it is passed on through the {@link Messenger} interface.
   */
  @Override
  public void run() {
    try {
      String message;
      while ((message = this.in.readLine()) != null) {
        MessageWorker messageWorker = new MessageWorker(message);
        messageWorker.setSource("CH", this.id).buildMessage();
        this.messenger.onMessage(messageWorker.getMessage(), this);
      }
    } catch (IOException e) {
      System.out.println("Connection error: " + e.getMessage());

    } finally {
      this.messenger.onDisconnect(this);
      close();
    }
  }

  /**
   * Sends a message to the client.
   *
   * @param message is the {@link String} message to send.
   * @throws IllegalArgumentException if {@code message} is {@code null}.
   */
  public synchronized void sendToClient(String message)  {
    // Guards.
    // Message cannot be null.
    if (message == null) {
      throw new IllegalArgumentException("Message to client cannot be null.");
    }

    this.out.println(message);
    this.lastSentMessage = message;
  }

  /**
   * Sets a {@link Socket} to {@link #socket}.
   *
   * @param socket is the {@link Socket} to set.
   * @throws IllegalArgumentException if {@code socket} is {@code null}.
   */
  private void setSocket(Socket socket) {
    // Guards.
    // Socket cannot be null.
    if (socket == null) {
      throw new IllegalArgumentException("Socket cannot be null.");
    }
    this.socket = socket;
  }

  /**
   * Sets a {@link BufferedReader} to {@link #in} with an {@link InputStreamReader}
   * using the input stream from {@link #socket}.
   *
   * @throws IOException if there is an issue with the input stream form {@link #socket}.
   */
  private void setBufferedReader() throws IOException {
    this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
  }

  /**
   * Sets a {@link PrintWriter} to {@link #out} using the output stream from {@link #socket}.
   *
   * @throws IOException if there is an issue with the output stream from {@link #socket}.
   */
  private void setPrintWriter() throws IOException {
    this.out = new PrintWriter(this.socket.getOutputStream(), true);
  }

  /**
   * Sets a {@link Messenger} interface to the class field {@link #messenger}.
   *
   * @param messenger is the interface to set.
   * @throws IllegalArgumentException if {@code messenger} is {@code null}.
   */
  private void setMessenger(Messenger<String, ClientHandler> messenger)  {
    if (messenger == null)  {
      throw new IllegalArgumentException("Messenger interface cannot be set to null.");
    }

    this.messenger = messenger;
  }

  /**
   * Sets the class field {@link #id}.
   *
   * @param id is the id to set.
   */
  private void setId(long id) {
    this.id = id;
  }

  /**
   * Returns the {@link #id}.
   *
   * @return {@link #id}.
   */
  public long getId() {
    return this.id;
  }

  /**
   * Returns the last message sent to the client.
   *
   * @return the last message sent to the client.
   */
  public String getLastSentMessage() {
    return this.lastSentMessage;
  }

  /**
   * Shuts down the {@link ClientHandler}, including its {@link Socket}.
   */
  public void close() {
    try {
      this.socket.close();
    } catch (IOException e) {
      // Ignore.
    }
  }
}
