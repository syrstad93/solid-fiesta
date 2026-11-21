package edu.ntnu.bidata.group1.utility;

import edu.ntnu.bidata.group1.network.server.AutomationEngine;
import edu.ntnu.bidata.group1.network.server.ClientDirectory;
import edu.ntnu.bidata.group1.network.server.Subscriptions;

/**
 * Keeps classes alive for when the server shuts down,
 * so that they can be used again when the server is restarted.
 *
 * <p>This class uses a singleton pattern, so it can be accessed by calling {@link #getInstance()}.
 * This keeps it alive unless manually reset with {@link #resetInstance()}.
 */
public class Vault {

  private static volatile Vault instance;

  private AutomationEngine automationEngine;
  private ClientDirectory clientDirectory;
  private Subscriptions subscriptions;

  /**
   * Instantiates the class.
   */
  private Vault() {
    // Nothing to set up.
  }

  /**
   * Returns the instance of this class, or creating a new one if it does not exist.
   *
   * @return the instance of this class.
   */
  public static Vault getInstance() {
    if (instance == null) {
      synchronized (Vault.class) {
        if (instance == null) {
          instance = new Vault();
        }
      }
    }
    return instance;
  }

  /**
   * Resets the instance of this class to {@code null}.
   */
  public static void resetInstance() {
    instance = null;
  }

  /**
   * Sets the {@link AutomationEngine} instance.
   *
   * @param automationEngine the {@link AutomationEngine} instance to set.
   * @throws IllegalArgumentException if {@code automationEngine} is {@code null}.
   */
  public void setAutomationEngine(AutomationEngine automationEngine) {
    if (automationEngine == null) {
      throw new IllegalArgumentException("AutomationEngine cannot be null.");
    }
    this.automationEngine = automationEngine;
  }

  /**
   * Returns the {@link AutomationEngine} instance.
   *
   * @return the {@link AutomationEngine} instance.
   */
  public AutomationEngine getAutomationEngine() {
    return this.automationEngine;
  }

  /**
   * Sets the {@link ClientDirectory} instance.
   *
   * @param clientDirectory the {@link ClientDirectory} instance to set.
   * @throws IllegalArgumentException if {@code clientDirectory} is {@code null}.
   */
  public void setClientDirectory(ClientDirectory clientDirectory) {
    if (clientDirectory == null) {
      throw new IllegalArgumentException("ClientDirectory cannot be null.");
    }
    this.clientDirectory = clientDirectory;
  }

  /**
   * Returns the {@link ClientDirectory} instance.
   *
   * @return the {@link ClientDirectory} instance.
   */
  public ClientDirectory getClientDirectory() {
    return this.clientDirectory;
  }

  /**
   * Sets the {@link Subscriptions} instance.
   *
   * @param subscriptions the {@link Subscriptions} instance to set.
   * @throws IllegalArgumentException if {@code subscriptions} is {@code null}.
   */
  public void setSubscriptions(Subscriptions subscriptions) {
    if (subscriptions == null) {
      throw new IllegalArgumentException("Subscriptions cannot be null.");
    }
    this.subscriptions = subscriptions;
  }

  /**
   * Returns the {@link Subscriptions} instance.
   *
   * @return the {@link Subscriptions} instance.
   */
  public Subscriptions getSubscriptions() {
    return this.subscriptions;
  }

  /**
   * Checks if the {@link AutomationEngine} is set.
   *
   * @return {@code true} if the {@link AutomationEngine} is set, {@code false} otherwise.
   */
  public boolean hasAutomationEngine() {
    return this.automationEngine != null;
  }

  /**
   * Checks if the {@link ClientDirectory} is set.
   *
   * @return {@code true} if the {@link ClientDirectory} is set, {@code false} otherwise.
   */
  public boolean hasClientDirectory() {
    return this.clientDirectory != null;
  }

  /**
   * Checks if the {@link Subscriptions} is set.
   *
   * @return {@code true} if the {@link Subscriptions} is set, {@code false} otherwise.
   */
  public boolean hasSubscriptions() {
    return this.subscriptions != null;
  }
}
