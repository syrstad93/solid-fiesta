package edu.ntnu.bidata.group1.gui.view;

import java.util.ArrayList;
import java.util.List;

import edu.ntnu.bidata.group1.utility.Permission;
import edu.ntnu.bidata.group1.utility.Role;

/**
 * Text shown in several views.
 */
public final class Texts {

  private Texts() {
  }

  /**
   * Describes a permission for people.
   *
   * @param permission the permission
   * @return for example {@code Manage rooms}
   */
  public static String describe(Permission permission) {
    return switch (permission) {
      case CONTROL_DEVICES -> "Control devices";
      case MANAGE_ROOMS -> "Manage rooms";
      case MANAGE_AUTOMATIONS -> "Manage automations";
      case MANAGE_DEVICES -> "Add & remove devices";
    };
  }

  /**
   * Lists what a role may do.
   *
   * @param role the role
   * @return for example {@code Control devices · Manage rooms}
   */
  public static String describe(Role role) {
    List<String> parts = new ArrayList<>();
    for (Permission permission : Permission.values()) {
      if (role.has(permission)) {
        parts.add(describe(permission));
      }
    }
    return String.join(" · ", parts);
  }

  /**
   * Turns a name such as {@code power} or {@code targetTemperature} into {@code Power} or
   * {@code Target temperature}.
   *
   * @param name the name
   * @return the readable name
   */
  public static String pretty(String name) {
    if (name == null || name.isEmpty()) {
      return "";
    }
    String spaced = name.replaceAll("([a-z])([A-Z])", "$1 $2").replace('-', ' ').toLowerCase();
    return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
  }

  /**
   * Shows a role in title case.
   *
   * @param role the role
   * @return for example {@code Owner}
   */
  public static String title(Role role) {
    return pretty(role.name());
  }
}
