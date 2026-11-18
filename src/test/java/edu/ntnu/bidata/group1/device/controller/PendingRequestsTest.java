package edu.ntnu.bidata.group1.device.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.ntnu.bidata.group1.utility.Protocol;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link PendingRequests}, which tracks requests sent to the server and
 * completes or fails each one when the matching server reply, error, or timeout arrives.
 * The tests cover matching confirmations by key and identifier, extracting the identifying
 * part of a reply value, routing server errors to the oldest matching request, and failing
 * requests that receive no answer in time.
 */
class PendingRequestsTest {

  /**
   * Verifies that a server confirmation completes only the pending request whose key matches the
   * confirmation.
   *
   * <p>Two {@code ROOM_ADD} requests are registered with keys {@code "10,1"} and {@code "10,2"}.
   * A {@code ROOM_ADDED} message for a different key ({@code "20,1"}) and a {@code ROOM_REMOVED}
   * message for an existing key ({@code "10,2"}) must not match either request. A {@code ROOM_ADDED}
   * message for key {@code "10,2"} must complete the second request, leave the first request pending,
   * and reduce the number of pending requests to one.
   *
   * @throws Exception if retrieving the completed future fails
   */
  @Test
  void confirmationCompletesTheMatchingRequest() throws Exception {
    PendingRequests pending = new PendingRequests();
    CompletableFuture<String> first = pending.add(Protocol.ROOM_ADD, Protocol.ROOM_ADDED, "10,1");
    CompletableFuture<String> second =
        pending.add(Protocol.ROOM_ADD, Protocol.ROOM_ADDED, "10,2");

    assertFalse(pending.onServerMessage(Protocol.ROOM_ADDED, "20,1"));
    assertFalse(pending.onServerMessage(Protocol.ROOM_REMOVED, "10,2"));
    assertTrue(pending.onServerMessage(Protocol.ROOM_ADDED, "10,2"));

    assertEquals("10,2", second.get());
    assertFalse(first.isDone());
    assertEquals(1, pending.size());
  }

  /**
   * Verifies that a pending {@code DEVICE_ADD} request is completed by a {@code DEVICE_ADDED}
   * server message whose device id matches the id of the request, and by no other message.
   *
   * <p>Messages carrying a different device id must not match the pending request and must
   * return {@code false}. The message carrying the matching id must return {@code true}, and the
   * request future must complete with the full message payload.
   *
   * @throws Exception if the pending request future cannot be retrieved
   */
  @Test
  void deviceAddedIsMatchedByTheDeviceIdAlone() throws Exception {
    PendingRequests pending = new PendingRequests();
    CompletableFuture<String> request =
        pending.add(Protocol.DEVICE_ADD, Protocol.DEVICE_ADDED, "5");

    assertFalse(pending.onServerMessage(Protocol.DEVICE_ADDED, "7,hall-heater,ONLINE"));
    assertFalse(pending.onServerMessage(Protocol.DEVICE_ADDED, "55,fan,ONLINE"));
    assertTrue(pending.onServerMessage(Protocol.DEVICE_ADDED, "5,kitchen-light,ONLINE"));
    assertEquals("5,kitchen-light,ONLINE", request.get());
  }

  /**
   * Verifies that an {@code AUTOMATION_ADDED} server message is matched to its pending
   * {@code AUTOMATION_ADD} request by the automation's definition fields, without
   * considering the leading automation id that the server assigns.
   *
   * <p>A message whose definition differs from the request (here, the comparison operator
   * {@code BELOW} instead of {@code ABOVE}) is not matched and leaves the request pending.
   * A message with a different assigned id but the same definition is matched, completes
   * the request, and the request's future yields the full message text, including the new id.
   *
   * @throws Exception if the pending request future cannot be retrieved
   */
  @Test
  void automationAddedIsMatchedWithoutItsNewId() throws Exception {
    PendingRequests pending = new PendingRequests();
    CompletableFuture<String> request = pending.add(Protocol.AUTOMATION_ADD,
        Protocol.AUTOMATION_ADDED, "3,temperature,ABOVE,25.0,1,power:true");

    assertFalse(pending.onServerMessage(Protocol.AUTOMATION_ADDED,
        "6,3,temperature,BELOW,25.0,1,power:true"));
    assertTrue(pending.onServerMessage(Protocol.AUTOMATION_ADDED,
        "7,3,temperature,ABOVE,25.0,1,power:true"));
    assertEquals("7,3,temperature,ABOVE,25.0,1,power:true", request.get());
  }

  /**
   * Verifies that {@link PendingRequests#aboutOf(Protocol, String)} returns the part of a
   * server message value that identifies the subject of the request, as determined by the
   * protocol of the message.
   *
   * <p>For a {@code DEVICE_ADDED} message, only the leading device id is returned. For an
   * {@code AUTOMATION_ADDED} message, the leading automation id assigned by the server is
   * dropped and the remaining definition fields are returned unchanged. For a {@code ROOM_ADDED}
   * message, the full value is returned when it consists of the room id and the device id. For a
   * {@code DEVICE_REMOVED} message whose value is only the device id, that id is returned as is.
   */
  @Test
  void aboutOfTakesTheRightPartOfTheValue() {
    assertEquals("5", PendingRequests.aboutOf(Protocol.DEVICE_ADDED, "5,kitchen-light,ONLINE"));
    assertEquals("3,temperature,ABOVE,25.0,1,power:true", PendingRequests.aboutOf(
        Protocol.AUTOMATION_ADDED, "7,3,temperature,ABOVE,25.0,1,power:true"));
    assertEquals("10,5", PendingRequests.aboutOf(Protocol.ROOM_ADDED, "10,5"));
    assertEquals("5", PendingRequests.aboutOf(Protocol.DEVICE_REMOVED, "5"));
  }

  /**
   * Verifies that an error reported by the server fails only the oldest pending request registered
   * under the key carried by that error.
   *
   * <p>Two requests are registered: a {@code ROOM_ADD} request with key {@code "10,1"} and a
   * {@code DEVICE_ADD} request with key {@code "9"}. An error for an unknown key
   * ({@code "AUTOMATION-ADD"}) must not match any request and must return {@code false}. An error
   * for the key {@code "DEVICE-ADD"} with the message {@code "Device 9 is already registered."} must
   * match the device request, return {@code true}, and fail that request with a
   * {@link RequestFailedException} carrying the error message. The room request must remain pending,
   * and exactly one pending request must remain.
   *
   * @throws ExecutionException if retrieving the failed future does not throw as expected
   */
  @Test
  void errorFailsTheOldestRequestWithThatKey() {
    PendingRequests pending = new PendingRequests();
    CompletableFuture<String> roomRequest =
        pending.add(Protocol.ROOM_ADD, Protocol.ROOM_ADDED, "10,1");
    CompletableFuture<String> deviceRequest =
        pending.add(Protocol.DEVICE_ADD, Protocol.DEVICE_ADDED, "9");

    assertFalse(pending.onError("AUTOMATION-ADD", "no such thing"));
    assertTrue(pending.onError("DEVICE-ADD", "Device 9 is already registered."));

    assertTrue(deviceRequest.isCompletedExceptionally());
    assertFalse(roomRequest.isDone());
    assertEquals(1, pending.size());
    ExecutionException e = assertThrows(ExecutionException.class, deviceRequest::get);
    assertTrue(e.getCause() instanceof RequestFailedException);
    assertEquals("Device 9 is already registered.", e.getCause().getMessage());
  }

  /**
   * Verifies that a pending request which never receives a matching server confirmation
   * is failed once the configured timeout elapses.
   *
   * <p>A {@link PendingRequests} instance is created with a timeout of 100 milliseconds, and a
   * {@code ROOM_ADD} request with key {@code "10,1"} is registered. No confirmation is delivered.
   * Waiting for the request future must throw an {@link ExecutionException} whose cause is a
   * {@link RequestFailedException} with a message stating that the server did not answer
   * {@code ROOM-ADD}. After the failure, the number of pending requests must be zero.
   *
   * @throws Exception if the test setup or waiting for the future fails unexpectedly
   */
  @Test
  void unansweredRequestsFailAfterTheTimeout() throws Exception {
    PendingRequests pending = new PendingRequests(100);
    CompletableFuture<String> request =
        pending.add(Protocol.ROOM_ADD, Protocol.ROOM_ADDED, "10,1");
    ExecutionException e = assertThrows(ExecutionException.class, request::get);
    assertTrue(e.getCause() instanceof RequestFailedException);
    assertTrue(e.getCause().getMessage().contains("did not answer ROOM-ADD"));
    assertEquals(0, pending.size());
  }

  /**
   * Verifies that a pending request whose confirmation arrives before its timeout is not failed
   * when the timeout period later elapses.
   *
   * <p>A {@code DEVICE_REMOVE} request is registered with a timeout of 100 milliseconds, and a
   * matching {@code DEVICE_REMOVED} server message with key {@code "5"} confirms it. After waiting
   * longer than the timeout, the request future must still complete normally with the key
   * {@code "5"}, and no pending requests must remain.
   *
   * @throws Exception if retrieving the completed future fails
   */
  @Test
  void aConfirmedRequestIsNotFailedByItsTimeoutLater() throws Exception {
    PendingRequests pending = new PendingRequests(100);
    CompletableFuture<String> request =
        pending.add(Protocol.DEVICE_REMOVE, Protocol.DEVICE_REMOVED, "5");
    assertTrue(pending.onServerMessage(Protocol.DEVICE_REMOVED, "5"));
    Thread.sleep(200);
    assertEquals("5", request.get());
    assertEquals(0, pending.size());
  }
}
