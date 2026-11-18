package edu.ntnu.bidata.group1.network.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Subscriptions}.
 *
 * <p>Each test covers one behavior of the class: keeping subscribers per device in
 * subscription order, ignoring duplicate subscriptions, removing a single subscription,
 * storing the latest value of each device property, and the different removal methods.
 * Those methods remove subscriptions to a device, subscriptions held by a client, the
 * stored state of a device, or everything related to a client. Each test starts with a
 * new, empty {@link Subscriptions} instance.
 *
 * <p>The tests check that removing subscriptions to a device keeps its stored state, that
 * removing a subscriber keeps the subscriptions made to it, and that removing a client
 * forgets all of its data.
 */
class SubscriptionsTest {

  private static final long LIGHT = 1;
  private static final long SENSOR = 2;
  private static final long OWNER = 3;
  private static final long GUEST = 4;

  private Subscriptions subscriptions;

  /**
   * Creates a new {@link Subscriptions} instance before each test method runs, so that every test
   * starts with no subscriptions and no stored device states.
   */
  @BeforeEach
  void setUp() {
    subscriptions = new Subscriptions();
  }

  /**
   * Verifies that subscribers are tracked separately for each device and that
   * their insertion order is preserved when retrieved.
   *
   * <p>Two clients subscribe to the same device and one client subscribes to a
   * different device. The subscribers of each device are then checked to be
   * returned in the order they subscribed, and a device with no subscriptions
   * is checked to yield an empty set.
   */
  @Test
  void subscribersAreKeptPerDeviceInOrder() {
    assertTrue(subscriptions.subscribe(OWNER, LIGHT));
    assertTrue(subscriptions.subscribe(GUEST, LIGHT));
    assertTrue(subscriptions.subscribe(OWNER, SENSOR));
    assertEquals(List.of(OWNER, GUEST), List.copyOf(subscriptions.subscribersOf(LIGHT)));
    assertEquals(Set.of(OWNER), subscriptions.subscribersOf(SENSOR));
    assertEquals(Set.of(), subscriptions.subscribersOf(99));
  }

  /**
   * Verifies that subscribing a client to a device a second time has no effect.
   *
   * <p>The client subscribes to the device once, then subscribes again. The second
   * call must return {@code false}, indicating that the client was already
   * subscribed, and the set of subscribers of the device must still contain only
   * that client.
   */
  @Test
  void subscribingTwiceIsHarmless() {
    subscriptions.subscribe(OWNER, LIGHT);
    assertFalse(subscriptions.subscribe(OWNER, LIGHT));
    assertEquals(Set.of(OWNER), subscriptions.subscribersOf(LIGHT));
  }

  /**
   * Verifies that unsubscribing one client from a device leaves the other client's
   * subscription to that device intact.
   *
   * <p>Two clients subscribe to the same device. The first client is then unsubscribed,
   * which must return {@code true}. A second unsubscribe call for the same client must
   * return {@code false}, since the subscription no longer exists. Finally, the set of
   * subscribers of the device must contain only the remaining client.
   */
  @Test
  void unsubscribeRemovesOnlyThatSubscription() {
    subscriptions.subscribe(OWNER, LIGHT);
    subscriptions.subscribe(GUEST, LIGHT);
    assertTrue(subscriptions.unsubscribe(OWNER, LIGHT));
    assertFalse(subscriptions.unsubscribe(OWNER, LIGHT));
    assertEquals(Set.of(GUEST), subscriptions.subscribersOf(LIGHT));
  }

  /**
   * Verifies that the state of a device keeps only the most recent value reported for each
   * property.
   *
   * <p>The light reports {@code powerOn} as {@code false}, then {@code value} as {@code 0}, then
   * {@code powerOn} as {@code true}. Reporting {@code powerOn} as {@code true} again must return
   * {@code false}, since the value did not change. The state of the light must then contain one
   * entry per property holding its latest value, while a device that never reported any state must
   * yield an empty map.
   */
  @Test
  void stateKeepsTheLatestValuePerProperty() {
    assertTrue(subscriptions.updateState(LIGHT, "powerOn", "false"));
    assertTrue(subscriptions.updateState(LIGHT, "value", "0"));
    assertTrue(subscriptions.updateState(LIGHT, "powerOn", "true"));
    assertFalse(subscriptions.updateState(LIGHT, "powerOn", "true"));
    assertEquals(Map.of("powerOn", "true", "value", "0"), subscriptions.stateOf(LIGHT));
    assertEquals(Map.of(), subscriptions.stateOf(SENSOR));
  }

  /**
   * Verifies that removing all subscriptions to a device does not discard the state stored for that
   * device.
   *
   * <p>A client subscribes to the light, and the light then reports {@code powerOn} as
   * {@code true}. Removing the subscriptions to the light must leave the light without any
   * subscribers, while its stored state must still contain the reported {@code powerOn} value.
   */
  @Test
  void removingSubscriptionsToDeviceKeepsItsState() {
    subscriptions.subscribe(OWNER, LIGHT);
    subscriptions.updateState(LIGHT, "powerOn", "true");
    subscriptions.removeSubscriptionsTo(LIGHT);
    assertEquals(Set.of(), subscriptions.subscribersOf(LIGHT));
    assertEquals(Map.of("powerOn", "true"), subscriptions.stateOf(LIGHT));
  }

  /**
   * Verifies that removing a client as a subscriber does not remove the subscriptions other clients
   * hold to it.
   *
   * <p>The owner subscribes to the light, and the guest subscribes to the owner. The owner then
   * reports a state value, is removed as a subscriber, and its state is cleared. The light must
   * have no subscribers once the owner is removed, while the guest must still be subscribed to the
   * owner. The owner's state must be empty after it is cleared.
   */
  @Test
  void removingSubscriberKeepsTheSubscriptionsToIt() {
    subscriptions.subscribe(OWNER, LIGHT);
    subscriptions.subscribe(GUEST, OWNER);
    subscriptions.updateState(OWNER, "type", "CONTROLLER");
    subscriptions.removeSubscriber(OWNER);
    subscriptions.clearState(OWNER);
    assertEquals(Set.of(), subscriptions.subscribersOf(LIGHT));
    assertEquals(Set.of(GUEST), subscriptions.subscribersOf(OWNER));
    assertEquals(Map.of(), subscriptions.stateOf(OWNER));
  }

  /**
   * Verifies that removing a client forgets everything about it.
   *
   * <p>The guest subscribes to the light and to the sensor, the owner subscribes to the light and
   * to the guest, and the guest reports a {@code type} state value. Removing the guest must leave
   * the owner as the only subscriber of the light, leave the sensor without any subscribers, leave
   * the guest without any subscribers, and leave the guest without any stored state.
   */
  @Test
  void removingClientForgetsEverythingAboutIt() {
    subscriptions.subscribe(OWNER, LIGHT);
    subscriptions.subscribe(GUEST, LIGHT);
    subscriptions.subscribe(GUEST, SENSOR);
    subscriptions.subscribe(OWNER, GUEST);
    subscriptions.updateState(GUEST, "type", "CONTROLLER");

    subscriptions.removeClient(GUEST);

    assertEquals(Set.of(OWNER), subscriptions.subscribersOf(LIGHT));
    assertEquals(Set.of(), subscriptions.subscribersOf(SENSOR));
    assertEquals(Set.of(), subscriptions.subscribersOf(GUEST));
    assertEquals(Map.of(), subscriptions.stateOf(GUEST));
  }
}
