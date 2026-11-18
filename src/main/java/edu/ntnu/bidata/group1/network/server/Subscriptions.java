package edu.ntnu.bidata.group1.network.server;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Keeps track of which clients are subscribed to which devices, and of the latest state of every
 * device.
 *
 * <p>Devices send their state to the server, one {@code STATE:<property>,<value>} message per
 * property. The server stores it here with {@link #updateState(long, String, String)} and sends
 * it on to every client returned by {@link #subscribersOf(long)}. A client that subscribes is
 * first sent the stored state from {@link #stateOf(long)}, so it does not have to wait for the
 * next change.
 *
 * <p>This class only keeps track; it does not send messages. That is done by
 * {@link ServerLogic}. All methods are synchronized, so messages from different clients can
 * arrive on different threads.
 */
public class Subscriptions {

  private final Map<Long, Set<Long>> subscribers = new HashMap<>();
  private final Map<Long, Map<String, String>> states = new HashMap<>();

  /**
   * Creates an empty list of subscriptions and states.
   */
  public Subscriptions() {
    // Nothing to set up.
  }

  /**
   * Subscribes a client to a device.
   *
   * @param subscriberId the client that wants the device's state
   * @param deviceId the device
   * @return {@code true} if the client was not subscribed to the device before
   */
  public synchronized boolean subscribe(long subscriberId, long deviceId) {
    return subscribers.computeIfAbsent(deviceId, id -> new LinkedHashSet<>()).add(subscriberId);
  }

  /**
   * Unsubscribes a client from a device.
   *
   * @param subscriberId the client
   * @param deviceId the device
   * @return {@code true} if the client was subscribed to the device
   */
  public synchronized boolean unsubscribe(long subscriberId, long deviceId) {
    Set<Long> set = subscribers.get(deviceId);
    if (set == null || !set.remove(subscriberId)) {
      return false;
    }
    if (set.isEmpty()) {
      subscribers.remove(deviceId);
    }
    return true;
  }

  /**
   * Returns the clients subscribed to a device.
   *
   * @param deviceId the device
   * @return a copy of the subscribers' ids, in the order they subscribed
   */
  public synchronized Set<Long> subscribersOf(long deviceId) {
    Set<Long> set = subscribers.get(deviceId);
    return set == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(set));
  }

  /**
   * Stores a device's latest value for one property.
   *
   * @param deviceId the device
   * @param property the property's name, for example {@code powerOn}
   * @param value the property's value, for example {@code true}
   * @return {@code true} if the value is different from the one stored before
   */
  public synchronized boolean updateState(long deviceId, String property, String value) {
    String previous = states.computeIfAbsent(deviceId, id -> new LinkedHashMap<>())
        .put(property, value);
    return !value.equals(previous);
  }

  /**
   * Returns a device's latest state.
   *
   * @param deviceId the device
   * @return a copy of every property the device has reported; empty if none
   */
  public synchronized Map<String, String> stateOf(long deviceId) {
    Map<String, String> state = states.get(deviceId);
    return state == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(state));
  }

  /**
   * Removes every subscription to a device, for example because it was unregistered. Its stored
   * state is kept: the device is still connected and only reports changes, so the state is
   * needed if the device is registered again.
   *
   * @param deviceId the device
   */
  public synchronized void removeSubscriptionsTo(long deviceId) {
    subscribers.remove(deviceId);
  }

  /**
   * Removes every subscription a client holds to other devices. Subscriptions to the client
   * itself are kept.
   *
   * @param subscriberId the client
   */
  public synchronized void removeSubscriber(long subscriberId) {
    subscribers.values().forEach(set -> set.remove(subscriberId));
    subscribers.values().removeIf(Set::isEmpty);
  }

  /**
   * Forgets a device's stored state, for example because it disconnected. It sends its whole
   * state again when it reconnects.
   *
   * @param deviceId the device
   */
  public synchronized void clearState(long deviceId) {
    states.remove(deviceId);
  }

  /**
   * Forgets a client completely, because it disconnected: its subscriptions to other devices,
   * the subscriptions to it, and its stored state.
   *
   * @param clientId the client
   */
  public synchronized void removeClient(long clientId) {
    subscribers.remove(clientId);
    states.remove(clientId);
    subscribers.values().forEach(set -> set.remove(clientId));
    subscribers.values().removeIf(Set::isEmpty);
  }
}
