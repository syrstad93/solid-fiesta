package edu.ntnu.bidata.group1.device.controller;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * The access level of a {@link Controller}. Each role can do everything the role below it can.
 *
 * <table>
 *   <caption>Permissions per role</caption>
 *   <tr><th>Role</th><th>Control devices</th><th>Manage rooms</th><th>Manage automations</th>
 *       <th>Manage devices</th></tr>
 *   <tr><td>GUEST</td><td>yes</td><td>no</td><td>no</td><td>no</td></tr>
 *   <tr><td>USER</td><td>yes</td><td>yes</td><td>yes</td><td>no</td></tr>
 *   <tr><td>OWNER</td><td>yes</td><td>yes</td><td>yes</td><td>yes</td></tr>
 * </table>
 *
 * <ul>
 *   <li>{@link #GUEST} (lowest): can only control devices, for example turn a light on or set
 *       its value.</li>
 *   <li>{@link #USER}: can also add devices to rooms and remove them from rooms, and add and
 *       remove automations.</li>
 *   <li>{@link #OWNER} (highest): can also add devices to the controller and remove them.</li>
 * </ul>
 */
public enum Role {
  /** Lowest access: can only control devices. */
  GUEST(EnumSet.of(Permission.CONTROL_DEVICES)),
  /** Can also manage rooms and automations. */
  USER(EnumSet.of(Permission.CONTROL_DEVICES, Permission.MANAGE_ROOMS,
      Permission.MANAGE_AUTOMATIONS)),
  /** Highest access: can also add and remove devices. */
  OWNER(EnumSet.allOf(Permission.class));

  private final Set<Permission> permissions;

  Role(Set<Permission> permissions) {
    this.permissions = Collections.unmodifiableSet(permissions);
  }

  /**
   * Returns whether this role grants the given permission.
   *
   * @param permission the permission to check
   * @return {@code true} if a controller with this role has the permission
   */
  public boolean has(Permission permission) {
    return permissions.contains(permission);
  }

  /**
   * Returns every permission this role grants.
   *
   * @return an unmodifiable set
   */
  public Set<Permission> getPermissions() {
    return permissions;
  }
}
