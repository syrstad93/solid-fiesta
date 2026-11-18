package edu.ntnu.bidata.group1.network.server;

import edu.ntnu.bidata.group1.utility.MessageWorker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Manages links between clients connected to the server.
 *
 * <p>
 *   A link consist of the following:
 * </p>
 * <ul>
 *   <li>
 *     A {@code source} client.
 *   </li>
 *   <li>
 *     A {@code destination} client.
 *   </li>
 *   <li>
 *     A {@code trigger} point where if the source client reaches a certain status,
 *     the destination client is sent a message.
 *   </li>
 *   <li>
 *     A {@code command} for a message that is sent to the destination client
 *     when the trigger point is reached.
 *   </li>
 *   <li>
 *     A {@code triggered} boolean that indicates whether the link has already been triggered.
 *     This is used to prevent sending the same message multiple times.
 *   </li>
 * </ul>
 *
 * <p>
 *   This class is a singleton, and can be accessed by calling {@link #getInstance()}.
 * </p>
 */
public class LinkedClients {

  private static volatile LinkedClients instance;

  private HashMap<ClientHandler, List<Link>> links;

  /**
   * Instantiates the class with an empty map.
   */
  private LinkedClients() {
    this.links = new HashMap<>();
  }

  /**
   * Returns the instance of this class, or creating a new one if it does not exist.
   *
   * @return the instance of this class.
   */
  public static LinkedClients getInstance() {
    if (instance == null) {
      synchronized (LinkedClients.class) {
        if (instance == null) {
          instance = new LinkedClients();
        }
      }
    }
    return instance;
  }

  /**
   * Adds a client as source. Initially, the client has no links to any other clients.
   *
   * @param source is the {@link ClientHandler} to add as a source client.
   * @throws IllegalArgumentException if {@code source} is {@code null}.
   * @throws IllegalArgumentException if {@code source} is already in the map.
   */
  public void addSource(ClientHandler source) {
    if (source == null) {
      throw new IllegalArgumentException("Source client cannot be null.");
    }
    if (this.links.containsKey(source)) {
      throw new IllegalArgumentException("Source client is already in the map.");
    }

    this.links.put(source, new ArrayList<>());
  }

  /**
   * Creates a link between a source client and a destination client.
   *
   * <p>
   *   The source client doesn't have to be added as a source client before creating a link.
   *   It will be added as a source client if it is not already.
   * </p>
   *
   * @param source is the {@link ClientHandler} that is the source of the link.
   * @param destination is the {@link ClientHandler} that is the destination of the link.
   * @param trigger is the status point at which to send the message to the destination client.
   * @param command is the command to send to the destination client.
   * @throws IllegalArgumentException if {@code source} is {@code null}.
   * @throws IllegalArgumentException if {@code destination} is {@code null}.
   * @throws IllegalArgumentException if {@code command} is {@code null}.
   * @throws IllegalArgumentException if {@code command} is blank.
   */
  public void addLink(ClientHandler source, ClientHandler destination, int trigger, String command) {
    if (source == null) {
      throw new IllegalArgumentException("Source client cannot be null.");
    }
    if (destination == null) {
      throw new IllegalArgumentException("Destination client cannot be null.");
    }
    if (command == null) {
      throw new IllegalArgumentException("Command cannot be null.");
    }
    if (command.isBlank()) {
      throw new IllegalArgumentException("Command cannot be blank.");
    }

    if (!this.links.containsKey(source)) {
      this.links.put(source, new ArrayList<>());
    }

    this.links.get(source).add(new Link(destination, trigger, command));
  }

  /**
   * Sets the {@code destination} client for the {@link Link} at the given {@code index}
   * of the list of links for the given {@code source} client.
   *
   * @param source is the {@link ClientHandler} that is the source of the link.
   * @param index is the index of the link in the list of links for the source client.
   * @param destination is the {@link ClientHandler} to send a message to.
   * @throws IllegalArgumentException if {@code source} is {@code null}.
   * @throws IllegalArgumentException if {@code source} is not in the map.
   * @throws IllegalArgumentException if {@code index} is out of bounds.
   * @throws IllegalArgumentException if {@code destination} is {@code null}.
   */
  public void editDestination(ClientHandler source, int index, ClientHandler destination) {
    if (source == null) {
      throw new IllegalArgumentException("Source client cannot be null.");
    }
    if (!this.links.containsKey(source)) {
      throw new IllegalArgumentException("Source client is not in the map.");
    }
    if (index < 0 || index >= this.links.get(source).size()) {
      throw new IllegalArgumentException("Index is out of bounds.");
    }
    if (destination == null) {
      throw new IllegalArgumentException("Destination client cannot be null.");
    }

    this.links.get(source).get(index).destination = destination;
  }

  /**
   * Sets the {@code trigger} point for the {@link Link} at the given {@code index}
   * of the list of links for the given {@code source} client.
   *
   * @param source is the {@link ClientHandler} that is the source of the link.
   * @param index is the index of the link in the list of links for the source client.
   * @param trigger is the status point at which to send the message to the destination client.
   * @throws IllegalArgumentException if {@code source} is {@code null}.
   * @throws IllegalArgumentException if {@code source} is not in the map.
   * @throws IllegalArgumentException if {@code index} is out of bounds.
   */
  public void editTrigger(ClientHandler source, int index, int trigger) {
    if (source == null) {
      throw new IllegalArgumentException("Source client cannot be null.");
    }
    if (!this.links.containsKey(source)) {
      throw new IllegalArgumentException("Source client is not in the map.");
    }
    if (index < 0 || index >= this.links.get(source).size()) {
      throw new IllegalArgumentException("Index is out of bounds.");
    }

    this.links.get(source).get(index).trigger = trigger;
  }

  /**
   * Sets the {@code command} for the {@link Link} at the given {@code index}
   * of the list of links for the given {@code source} client.
   *
   * @param source is the {@link ClientHandler} that is the source of the link.
   * @param index is the index of the link in the list of links for the source client.
   * @param command is the command to send to the destination client.
   * @throws IllegalArgumentException if {@code source} is {@code null}.
   * @throws IllegalArgumentException if {@code source} is not in the map.
   * @throws IllegalArgumentException if {@code index} is out of bounds.
   * @throws IllegalArgumentException if {@code command} is {@code null}.
   * @throws IllegalArgumentException if {@code command} is blank.
   */
  public void editCommand(ClientHandler source, int index, String command) {
    if (source == null) {
      throw new IllegalArgumentException("Source client cannot be null.");
    }
    if (!this.links.containsKey(source)) {
      throw new IllegalArgumentException("Source client is not in the map.");
    }
    if (index < 0 || index >= this.links.get(source).size()) {
      throw new IllegalArgumentException("Index is out of bounds.");
    }
    if (command == null) {
      throw new IllegalArgumentException("Command cannot be null.");
    }
    if (command.isBlank()) {
      throw new IllegalArgumentException("Command cannot be blank.");
    }

    this.links.get(source).get(index).command = command;
  }

  /**
   * Checks is the {@link Link} at the given {@code index} of the list of links for the given
   * {@code source} client is triggered or not.
   *
   * @param source is the {@link ClientHandler} that is the source of the link.
   * @param index is the index of the link in the list of links for the source client.
   * @return {@code true} if the link is triggered, {@code false} otherwise.
   * @throws IllegalArgumentException if {@code source} is {@code null}.
   * @throws IllegalArgumentException if {@code source} is not in the map.
   * @throws IllegalArgumentException if {@code index} is out of bounds.
   */
  public boolean linkIsTriggered(ClientHandler source, int index) {
    if (source == null) {
      throw new IllegalArgumentException("Source client cannot be null.");
    }
    if (!this.links.containsKey(source)) {
      throw new IllegalArgumentException("Source client is not in the map.");
    }
    if (index < 0 || index >= this.links.get(source).size()) {
      throw new IllegalArgumentException("Index is out of bounds.");
    }

    return this.links.get(source).get(index).triggered;
  }

  /**
   * Removes the {@link Link} at the given {@code index}
   * of the list of links for the given {@code source} client.
   *
   * @param source is the {@link ClientHandler} that is the source of the link.
   * @param index is the index of the link in the list of links for the source client.
   * @throws IllegalArgumentException if {@code source} is {@code null}.
   * @throws IllegalArgumentException if {@code source} is not in the map.
   * @throws IllegalArgumentException if {@code index} is out of bounds.
   */
  public void removeLink(ClientHandler source, int index) {
    if (source == null) {
      throw new IllegalArgumentException("Source client cannot be null.");
    }
    if (!this.links.containsKey(source)) {
      throw new IllegalArgumentException("Source client is not in the map.");
    }
    if (index < 0 || index >= this.links.get(source).size()) {
      throw new IllegalArgumentException("Index is out of bounds.");
    }

    this.links.get(source).remove(index);
  }

  /**
   * Removes a source client and all its links from the map.
   *
   * @param source is the {@link ClientHandler} that is the source of the links.
   * @throws IllegalArgumentException if {@code source} is {@code null}.
   * @throws IllegalArgumentException if {@code source} is not in the map.
   */
  public void removeSource(ClientHandler source) {
    if (source == null) {
      throw new IllegalArgumentException("Source client cannot be null.");
    }
    if (!this.links.containsKey(source)) {
      throw new IllegalArgumentException("Source client is not in the map.");
    }

    this.links.remove(source);
  }

  /**
   * Returns the number of links for the given {@code source} client.
   *
   * @param source is the {@link ClientHandler} that is the source of the links.
   * @return the number of links for the given {@code source} client.
   * @throws IllegalArgumentException if {@code source} is {@code null}.
   * @throws IllegalArgumentException if {@code source} is not in the map.
   */
  public int numberOfLinks(ClientHandler source) {
    if (source == null) {
      throw new IllegalArgumentException("Source client cannot be null.");
    }
    if (!this.links.containsKey(source)) {
      throw new IllegalArgumentException("Source client is not in the map.");
    }

    return this.links.get(source).size();
  }

  /**
   * Returns the total number of links from all source clients.
   *
   * @return the total number of links from all source clients.
   */
  public int numberOfLinks() {
    int total = 0;
    for (List<Link> linkList : this.links.values()) {
      total += linkList.size();
    }
    return total;
  }

  /**
   * Returns the number of source clients in the map.
   *
   * @return the number of source clients in the map.
   */
  public int numberOfSources() {
    return this.links.size();
  }

  /**
   * Checks if the status condition for the given {@code source} client is such that it triggers
   * the {@code command} of a {@link Link} to be sent to the {@code destination} client.
   *
   * <p>
   *   If the conditions are met, the {@code command} is sent to the {@code destination} client,
   *   and the link is marked as triggered.
   *   If the link is already marked as triggered, the command is not sent again.
   * </p>
   *
   * <p>
   *   If the link is already triggered,
   *   but the status condition is no longer met,
   *   the link is reset to untriggered.
   * </p>
   *
   * @param source is the {@link ClientHandler} that is the source of the links.
   * @throws IllegalArgumentException if {@code source} is {@code null}.
   * @throws IllegalArgumentException if {@code source} is not in the map.
   */
  public void checkLinkTriggersAndSendCommandIfTriggered(ClientHandler source) {
    if (source == null) {
      throw new IllegalArgumentException("Source client cannot be null.");
    }
    if (!this.links.containsKey(source)) {
      throw new IllegalArgumentException("Source client is not in the map.");
    }

    int currentStatus = ClientStatusMap.getInstance().getStatus(source);

    for (Link link : this.links.get(source)) {
      if (currentStatus >= link.trigger && !link.triggered) {

        MessageWorker messageWorker = new MessageWorker()
            .setSource("CH", source.getId())
            .setDestination("CH", link.destination.getId())
            .setCommand(link.command)
            .buildMessage();

        link.destination.sendToClient(messageWorker.getMessage());
        link.triggered = true;
      }
      if (currentStatus < link.trigger && link.triggered) {
        link.triggered = false;
      }
    }
  }

  /**
   * Resets the instance of this class, allowing for a new instance to be created.
   *
   * <p>
   *   All links and source clients are deleted.
   * </p>
   */
  public void resetInstance() {
    instance = null;
  }

  /**
   * A link set-up for a {@code source} client.
   *
   * <p>
   *   Contains the {@code destination} client,
   *   the {@code trigger} point,
   *   and the {@code command} to send to the destination client.
   * </p>
   */
  private class Link {
    ClientHandler destination;
    int trigger;
    String command;
    boolean triggered;

    /**
     * Instantiates a link with the given parameters.
     *
     * @param destination is the {@link ClientHandler} to send a message to.
     * @param trigger is the status point at which to send the message.
     * @param command is the command to send to the destination client.
     */
    public Link(ClientHandler destination, int trigger, String command) {
      this.destination = destination;
      this.trigger = trigger;
      this.command = command;
      this.triggered = false;
    }
  }
}
