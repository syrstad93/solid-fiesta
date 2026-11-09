package edu.ntnu.bidata.group1.network.server;

/**
 * Allows implementers to receive messages from clients that calls the interface.
 *
 * @param <MessageType> is the datatype of the messages being sent.
 * @param <ClientType> is the datatype of the client sending messages.
 */
public interface Messenger<MessageType, ClientType> {

  /**
   * Broadcasts the given message to all listeners.
   *
   * @param message is the message to send.
   * @param sender is the sender object.
   */
  void onMessage(MessageType message, ClientType sender);

  /**
   * Broadcasts that a client has disconnected.
   *
   * @param client is the client that has disconnected.
   */
  void onDisconnect(ClientType client);
}
