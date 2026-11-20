package edu.ntnu.bidata.group1.utility;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the permission levels defined by {@link Role}.
 *
 * <p>The tests check which {@link Permission}s each role grants through
 * {@link Role#has(Permission)},
 * that the owner role grants every permission, and that each role includes all permissions of the
 * role below it.
 */
class RoleTest {

  /**
   * Verifies that the {@link Role#GUEST} role grants only the {@link Permission#CONTROL_DEVICES}
   * permission. The guest role must not grant {@link Permission#MANAGE_ROOMS},
   * {@link Permission#MANAGE_DEVICES}, or {@link Permission#MANAGE_AUTOMATIONS}.
   */
  @Test
  void guestCanOnlyControlDevices() {
    assertTrue(Role.GUEST.has(Permission.CONTROL_DEVICES));
    assertFalse(Role.GUEST.has(Permission.MANAGE_ROOMS));
    assertFalse(Role.GUEST.has(Permission.MANAGE_DEVICES));
    assertFalse(Role.GUEST.has(Permission.MANAGE_AUTOMATIONS));
  }

  /**
   * Verifies that the {@link Role#USER} role grants the {@link Permission#CONTROL_DEVICES},
   * {@link Permission#MANAGE_ROOMS}, and {@link Permission#MANAGE_AUTOMATIONS} permissions.
   * The user role must not grant {@link Permission#MANAGE_DEVICES}.
   */
  @Test
  void userCanControlDevicesAndManageRoomsAndAutomations() {
    assertTrue(Role.USER.has(Permission.CONTROL_DEVICES));
    assertTrue(Role.USER.has(Permission.MANAGE_ROOMS));
    assertTrue(Role.USER.has(Permission.MANAGE_AUTOMATIONS));
    assertFalse(Role.USER.has(Permission.MANAGE_DEVICES));
  }

  /**
   * Verifies that the {@link Role#OWNER} role grants every value of {@link Permission}.
   * Iterates over all permissions and asserts that {@link Role#OWNER} has each one.
   */
  @Test
  void ownerCanDoEverything() {
    for (Permission permission : Permission.values()) {
      assertTrue(Role.OWNER.has(permission));
    }
  }

  /**
   * Verifies that each role grants every permission granted by the role below it.
   * The {@link Role#USER} role must include all permissions of {@link Role#GUEST}, and
   * the {@link Role#OWNER} role must include all permissions of {@link Role#USER}.
   */
  @Test
  void eachRoleCanDoEverythingTheRoleBelowCan() {
    assertTrue(Role.USER.getPermissions().containsAll(Role.GUEST.getPermissions()));
    assertTrue(Role.OWNER.getPermissions().containsAll(Role.USER.getPermissions()));
  }
}
