package edu.ntnu.bidata.group1.network.server;

/**
 * Lets a class be told about new connections, incoming messages and lost connections.
 *
 * <p>The server implements it to receive messages from its {@link ClientHandler}s, and every
 * device implements it to receive messages from its connection to the server.
 *
 * @param <MessageType> is the datatype of the messages being sent.
 * @param <ClientType> is the datatype of the client sending messages.
 */
public interface Messenger<MessageType, ClientType> {

  /**
   * Called right after a connection has been made, the first time and after every reconnect,
   * before anything else is sent on it. A device uses it to introduce itself with
   * {@code HELLO}. Does nothing unless overridden.
   *
   * @param client is the connection that was just made.
   */
  default void onConnect(ClientType client) {
    // Nothing to do by default.
  }

  /**
   * Called when a message arrives.
   *
   * @param message is the message that arrived.
   * @param sender is the connection it arrived on.
   */
  void onMessage(MessageType message, ClientType sender);

  /**
   * Called when a connection is lost.
   *
   * @param client is the connection that was lost.
   */
  void onDisconnect(ClientType client);
}
