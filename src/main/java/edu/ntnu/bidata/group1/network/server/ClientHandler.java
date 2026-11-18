package edu.ntnu.bidata.group1.network.server;

import edu.ntnu.bidata.group1.utility.MessageWorker;
import edu.ntnu.bidata.group1.utility.Protocol;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Handles a {@link Socket} connection to a remote client.
 * This includes input/output streams and data interchange with the client.
 */
public class ClientHandler implements Runnable  {

  private Socket socket;
  private BufferedReader in;
  private PrintWriter out;

  private Messenger<String, ClientHandler> messenger;

  private volatile long id;
  private volatile long lastSeen = System.currentTimeMillis();

  /**
   * Instantiates this class by setting a {@link Socket}
   * that is in communication with a remote client,
   * a {@link BufferedReader} that receives input from the {@link Socket},
   * and a {@link PrintWriter} that sends output to the {@link Socket}.
   *
   * @param socket is the {@link Socket} in communication with a remote client.
   * @param id is a unique ID for this {@link ClientHandler}.
   * @param messenger is an interface where incoming messages are passed on through.
   * @throws IOException if the socket's input or output stream cannot be opened.
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
   *
   * <p>If one message cannot be handled (for example because it is malformed, the command is
   * unknown, or the destination does not exist), the client is sent {@code ERROR:<reason>} and
   * the handler keeps reading. Only a broken connection ends the loop.
   */
  @Override
  public void run() {
    try {
      String message;
      while ((message = this.in.readLine()) != null) {
        this.lastSeen = System.currentTimeMillis();
        try {
          MessageWorker messageWorker = new MessageWorker(message);
          messageWorker.setSource("CH", this.id).buildMessage();
          this.messenger.onMessage(messageWorker.getMessage(), this);
        } catch (RuntimeException e) {
          System.out.println("Could not handle message from client " + this.id + " ("
              + message + "): " + e.getMessage());
          sendError(message, e);
        }
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
  }

  /**
   * Tells the client that one of its messages could not be handled, with
   * {@code ERROR:<request>,<reason>}, where {@code <request>} is the key of the rejected message.
   *
   * @param message the message that could not be handled.
   * @param e the error that occurred.
   */
  private void sendError(String message, RuntimeException e) {
    String requestKey;
    try {
      requestKey = new MessageWorker(message).getCommandKey();
    } catch (IllegalArgumentException malformed) {
      requestKey = null;
    }
    MessageWorker error = new MessageWorker()
        .setDestination("CH", this.id)
        .setSource(MessageWorker.SERVER)
        .setCommand(Protocol.error(requestKey, e.getMessage()))
        .buildMessage();
    sendToClient(error.getMessage());
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
   * using the input stream from {@link #socket}. Messages are read as UTF-8, the same encoding
   * clients send with.
   *
   * @throws IOException if there is an issue with the input stream form {@link #socket}.
   */
  private void setBufferedReader() throws IOException {
    this.in = new BufferedReader(
        new InputStreamReader(this.socket.getInputStream(), StandardCharsets.UTF_8));
  }

  /**
   * Sets a {@link PrintWriter} to {@link #out} using the output stream from {@link #socket}.
   * Messages are written as UTF-8, the same encoding clients read with.
   *
   * @throws IOException if there is an issue with the output stream from {@link #socket}.
   */
  private void setPrintWriter() throws IOException {
    this.out = new PrintWriter(
        new OutputStreamWriter(this.socket.getOutputStream(), StandardCharsets.UTF_8), true);
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
   * Gives this connection another id. Used when a client that reconnects introduces itself with
   * a name it has used before, so that it gets its old id back.
   *
   * @param id is the new id.
   */
  void changeId(long id) {
    this.id = id;
  }

  /**
   * Returns when the last message from the client arrived.
   *
   * @return the time, in milliseconds since 1970, as given by {@link System#currentTimeMillis()}.
   */
  public long getLastSeen() {
    return this.lastSeen;
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
