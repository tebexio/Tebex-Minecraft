package io.tebex.minecraft.commands;

import static org.junit.jupiter.api.Assertions.*;

import io.tebex.minecraft.runtime.MockPluginPlatform;
import org.junit.jupiter.api.Test;

class BuyAccessTest {
  @Test
  void sharesConsoleSetupPermissionAndAdminRules() {
    MockPluginPlatform host =
        new MockPluginPlatform() {
          @Override
          public boolean hasPermission(String name, String permission) {
            return name.equals("Admin") && permission.equals("tebex.admin")
                || name.equals("Buyer") && permission.equals("tebex.buy");
          }
        };
    try {
      assertTrue(host.buyAccessError(null).contains("console"));
      assertTrue(host.buyAccessError("Denied").contains("permission"));
      assertTrue(host.buyAccessError("Buyer").contains("setup"));
      host.setSetup(true);
      assertNull(host.buyAccessError("Buyer"));
      assertNull(host.buyAccessError("Admin"));
      assertNotNull(host.buyAccessError("Denied"));
    } finally {
      host.shutdown();
    }
  }

  @Test
  void permissionRegistrationPreservesPlatformInfoDefaults() {
    assertFalse(PermissionDefaults.nodes(false).get("info"));
    assertTrue(PermissionDefaults.nodes(true).get("info"));
    assertFalse(PermissionDefaults.nodes(true).get("admin"));
    assertEquals(PermissionDefaults.nodes(false).keySet(), PermissionDefaults.nodes(true).keySet());
  }
}
