package edu.ntnu.bidata.group1.device.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import edu.ntnu.bidata.group1.utility.Protocol;

/**
 * The requests a {@link Controller} has sent to the server and is still waiting for an answer
 * to, each with the future that is completed by that answer.
 *
 * <p>Each request is described by three things, see
 * {@link #add(Protocol, Protocol, String)}:
 * <ul>
 *   <li>the request that was sent, for example {@code DEVICE-ADD},</li>
 *   <li>the reply that confirms it, for example {@code DEVICE-ADDED}, and</li>
 *   <li>what that reply must be about, for example device {@code 5}.</li>
 * </ul>
 *
 * <p>A request is answered in one of two ways:
 * <ul>
 *   <li>A confirmation, such as {@code DEVICE-ADDED:5,kitchen-light,ONLINE} for
 *       {@code DEVICE-ADD:5}. The oldest waiting request that expects this reply about the same
 *       thing completes, with the reply's value.</li>
 *   <li>A refusal, {@code ERROR:<request>,<reason>}. The oldest waiting request with that key,
 *       for example {@code ROOM-ADD}, fails with a {@link RequestFailedException}. The server
 *       handles a client's messages in the order they were sent, so this is the right one.</li>
 * </ul>
 * A request that is answered neither way in time, {@value #TIMEOUT_MS} milliseconds by default,
 * fails with a {@link RequestFailedException} too.
 *
 * <p>Futures are completed on the connection's listener thread. Code that runs when a future
 * completes, such as {@code thenAccept}, must not block, for example by calling {@code get()} on
 * another request's future, because no more messages arrive while it runs.
 */
class PendingRequests {

  /** How long the server has to answer a request by default, in milliseconds. */
  static final long TIMEOUT_MS = 5000;

  // How long the server has to answer a request, in milliseconds.
  private final long timeoutMs;

  // The waiting requests, oldest first.
  private final List<Pending> pending = new ArrayList<>();

  /**
   * Creates an empty list, where the server has {@value #TIMEOUT_MS} milliseconds to answer.
   */
  PendingRequests() {
    this(TIMEOUT_MS);
  }

  /**
   * Creates an empty list with the given time limit.
   *
   * @param timeoutMs how long the server has to answer a request, in milliseconds
   */
  PendingRequests(long timeoutMs) {
    this.timeoutMs = timeoutMs;
  }

  /**
   * Starts waiting for the answer to a request. For example, after sending
   * {@code DEVICE-ADD:5}, wait for {@code DEVICE-ADDED} about {@code 5}:
   * <pre>{@code
   * pending.add(Protocol.DEVICE_ADD, Protocol.DEVICE_ADDED, "5");
   * }</pre>
   * What a reply is about is worked out by {@link #aboutOf(Protocol, String)}.
   *
   * @param request the request that was sent, for example {@link Protocol#DEVICE_ADD}; an
   *     {@code ERROR} for this key fails the request
   * @param reply the reply that confirms the request, for example {@link Protocol#DEVICE_ADDED}
   * @param about what the reply must be about, for example {@code 5} for device 5, or
   *     {@code 10,5} for device 5 in room 10
   * @return a future that completes with the confirming reply's value, for example
   *     {@code 5,kitchen-light,ONLINE}
   */
  CompletableFuture<String> add(Protocol request, Protocol reply, String about) {
    Pending entry = new Pending(request, reply, about);
    synchronized (this) {
      pending.add(entry);
    }
    String timeoutReason = "The server did not answer " + request.getKey() + " within "
        + timeoutMs + " ms.";
    CompletableFuture.delayedExecutor(timeoutMs, TimeUnit.MILLISECONDS)
        .execute(() -> fail(entry, timeoutReason));
    return entry.future;
  }

  /**
   * Completes the oldest waiting request that the given message from the server confirms.
   *
   * @param protocol the message's key, for example {@link Protocol#DEVICE_ADDED}
   * @param value the message's value, for example {@code 5,kitchen-light,ONLINE}
   * @return {@code true} if a request was completed
   */
  boolean onServerMessage(Protocol protocol, String value) {
    String about = aboutOf(protocol, value);
    Pending match = null;
    synchronized (this) {
      for (Pending entry : pending) {
        if (entry.reply == protocol && entry.about.equals(about)) {
          match = entry;
          break;
        }
      }
      pending.remove(match);
    }
    // Completed outside the lock: code waiting on the future runs straight away, on this thread.
    return match != null && match.future.complete(value);
  }

  /**
   * Fails the oldest waiting request with the given key.
   *
   * @param requestKey the key of the refused request, as given in {@code ERROR:<request>,...}
   * @param reason the server's reason
   * @return {@code true} if a waiting request was failed
   */
  boolean onError(String requestKey, String reason) {
    Pending match = null;
    synchronized (this) {
      for (Pending entry : pending) {
        if (entry.request.getKey().equals(requestKey)) {
          match = entry;
          break;
        }
      }
    }
    return match != null && fail(match, reason);
  }

  /**
   * Returns how many requests are still waiting for an answer.
   *
   * @return the number of waiting requests
   */
  synchronized int size() {
    return pending.size();
  }

  /**
   * Returns what a reply from the server is about, so it can be matched to a request.
   *
   * <ul>
   *   <li>{@code DEVICE-ADDED:5,kitchen-light,ONLINE} is about {@code 5}: the device's name and
   *       status are not part of it.</li>
   *   <li>{@code AUTOMATION-ADDED:7,3,temperature,...} is about {@code 3,temperature,...}: the
   *       request did not know the id 7 the server would give the automation.</li>
   *   <li>Every other reply is about its whole value, for example {@code ROOM-ADDED:10,5} is
   *       about {@code 10,5}, and {@code DEVICE-REMOVED:5} about {@code 5}.</li>
   * </ul>
   *
   * @param reply the reply's key
   * @param value the reply's value
   * @return what the reply is about
   */
  static String aboutOf(Protocol reply, String value) {
    int comma = value.indexOf(',');
    if (reply == Protocol.DEVICE_ADDED && comma >= 0) {
      return value.substring(0, comma);
    }
    if (reply == Protocol.AUTOMATION_ADDED && comma >= 0) {
      return value.substring(comma + 1);
    }
    return value;
  }

  /**
   * Takes a request off the list and fails it, unless it has already been answered.
   *
   * @param entry the request
   * @param reason why it failed
   * @return {@code true} if the request was still waiting
   */
  private boolean fail(Pending entry, String reason) {
    synchronized (this) {
      pending.remove(entry);
    }
    return entry.future.completeExceptionally(new RequestFailedException(reason));
  }

  /**
   * One request that is waiting for an answer.
   */
  private static class Pending {
    private final Protocol request;
    private final Protocol reply;
    private final String about;
    private final CompletableFuture<String> future = new CompletableFuture<>();

    Pending(Protocol request, Protocol reply, String about) {
      this.request = request;
      this.reply = reply;
      this.about = about;
    }
  }
}
