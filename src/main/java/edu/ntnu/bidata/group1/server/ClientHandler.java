package edu.ntnu.bidata.group1.server;

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

  /**
   * Instantiates this class by setting a {@link Socket}
   * that is in communication with a remote client,
   * a {@link BufferedReader} that receives input from the {@link Socket},
   * and a {@link PrintWriter} that sends output to the {@link Socket}.
   *
   * @param socket is the {@link Socket} in communication with a remote client.
   */
  public ClientHandler(Socket socket, Messenger<String, ClientHandler> messenger)
      throws IOException {
    this.messenger = messenger;
    setSocket(socket);
    setBufferedReader();
    setPrintWriter();
  }

  /**
   * Maintains a loop that waits to receive commands from the client. When a message is received,
   * it is passed on through the {@link Messenger} interface.
   */
  @Override
  public void run() {
    try {
      String command;
      while ((command = this.in.readLine()) != null) {
        this.messenger.onMessage(command, this);
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
