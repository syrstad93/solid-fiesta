package edu.ntnu.bidata.group1.network.server;

import edu.ntnu.bidata.group1.utility.Automation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stores the server's automation rules and decides when they fire.
 *
 * <p>This class only keeps track of rules and readings; it does not send messages. The
 * {@link ServerLogic} feeds it sensor readings with {@link #onReading(long, String, double)}, and
 * sends the commands of the automations it returns.
 *
 * <p>An automation fires once when its condition <em>becomes</em> true. It does not fire again
 * on later readings while the condition stays true, but is ready to fire again as soon as a
 * reading no longer meets the condition.
 *
 * <p>All methods are synchronized, so readings from different sensors can arrive on different
 * threads.
 */
public class AutomationEngine {

  private final Map<Long, Automation> automations = new LinkedHashMap<>();
  private final Set<Long> active = new HashSet<>();
  private final Map<String, Double> latestReadings = new HashMap<>();
  private long nextId = 1;

  /**
   * Creates an engine with no automations and no readings.
   */
  public AutomationEngine() {
    // Nothing to set up.
  }

  /**
   * Adds an automation and gives it the next free id.
   *
   * @param automation the automation to add; its id is ignored
   * @return the stored automation, with its id
   * @throws IllegalArgumentException if {@code automation} is null
   */
  public synchronized Automation add(Automation automation) {
    if (automation == null) {
      throw new IllegalArgumentException("Automation cannot be null.");
    }
    Automation stored = automation.withId(nextId++);
    automations.put(stored.getId(), stored);
    return stored;
  }

  /**
   * Checks a newly added automation against the latest known reading of its sensor, so it does
   * not have to wait for the next reading.
   *
   * @param automationId the automation's id
   * @return the automation if it fires now, or an empty list
   */
  public synchronized List<Automation> evaluateNow(long automationId) {
    Automation automation = automations.get(automationId);
    if (automation == null) {
      return List.of();
    }
    Double latest = latestReadings.get(key(automation.getSensorId(), automation.getReading()));
    if (latest == null) {
      return List.of();
    }
    return update(automation, latest) ? List.of(automation) : List.of();
  }

  /**
   * Returns the latest value a sensor reported for a reading.
   *
   * @param sensorId the sensor's client id
   * @param reading the reading's name
   * @return the value, or {@code NaN} if the sensor has not reported this reading
   */
  public synchronized double latestReading(long sensorId, String reading) {
    return latestReadings.getOrDefault(key(sensorId, reading), Double.NaN);
  }

  /**
   * Removes an automation.
   *
   * @param id the automation's id
   * @return the removed automation
   * @throws IllegalArgumentException if no automation has this id
   */
  public synchronized Automation remove(long id) {
    Automation removed = automations.remove(id);
    if (removed == null) {
      throw new IllegalArgumentException("No automation with id " + id + ".");
    }
    active.remove(id);
    return removed;
  }

  /**
   * Removes every automation that uses the given client as sensor or as target, for example
   * because the device was unregistered.
   *
   * @param clientId the client's id
   * @return the removed automations
   */
  public synchronized List<Automation> removeInvolving(long clientId) {
    List<Automation> removed = new ArrayList<>();
    for (Automation automation : new ArrayList<>(automations.values())) {
      if (automation.involves(clientId)) {
        automations.remove(automation.getId());
        active.remove(automation.getId());
        removed.add(automation);
      }
    }
    latestReadings.keySet().removeIf(key -> key.startsWith(clientId + ":"));
    return removed;
  }

  /**
   * Records a sensor reading and returns the automations that fire because of it.
   *
   * @param sensorId the sensor's client id
   * @param reading the reading's name, for example {@code temperature}
   * @param value the measured value
   * @return the automations whose condition just became true; their commands should be sent
   */
  public synchronized List<Automation> onReading(long sensorId, String reading, double value) {
    latestReadings.put(key(sensorId, reading), value);
    List<Automation> fired = new ArrayList<>();
    for (Automation automation : automations.values()) {
      if (automation.watches(sensorId, reading) && update(automation, value)) {
        fired.add(automation);
      }
    }
    return fired;
  }

  /**
   * Returns every automation, in the order they were added.
   *
   * @return a copy of the list
   */
  public synchronized List<Automation> getAll() {
    return new ArrayList<>(automations.values());
  }

  /**
   * Updates whether an automation's condition is met, and returns whether it should fire.
   *
   * @param automation the automation to update
   * @param value the latest reading of its sensor
   * @return {@code true} if the condition just became true
   */
  private boolean update(Automation automation, double value) {
    boolean met = automation.getCondition().isMet(value, automation.getThreshold());
    if (met) {
      return active.add(automation.getId());
    }
    active.remove(automation.getId());
    return false;
  }

  private static String key(long sensorId, String reading) {
    return sensorId + ":" + reading;
  }
}
