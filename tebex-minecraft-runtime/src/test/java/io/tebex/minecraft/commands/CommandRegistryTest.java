package io.tebex.minecraft.commands;

import static org.junit.jupiter.api.Assertions.*;

import io.tebex.minecraft.runtime.MockPluginPlatform;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class CommandRegistryTest {
  @Test
  void proxyRestrictionsAreInstanceLocalAndAppliedToHelp() {
    MockPluginPlatform proxy = new MockPluginPlatform(), server = new MockPluginPlatform();
    try {
      proxy.getCommands().setRestrictedToCommands("help", "secret");
      assertFalse(proxy.getCommands().getCommands().containsKey("ban"));
      assertTrue(server.getCommands().getCommands().containsKey("ban"));
      AtomicReference<CompletableFuture<String[]>> response = new AtomicReference<>();
      proxy
          .getCommands()
          .process(
              Context.from(true, "", null, "tebex help", "", null, new String[] {"help"}),
              response::set);
      String output = String.join("\n", response.get().join());
      assertTrue(output.contains("/tebex secret"));
      assertFalse(output.contains("/tebex ban"));
    } finally {
      proxy.shutdown();
      server.shutdown();
    }
  }

  @Test
  void optionalVelocityBanMetadataDoesNotChangeOtherInstances() {
    MockPluginPlatform proxy = new MockPluginPlatform(), server = new MockPluginPlatform();
    try {
      proxy.getCommands().useOptionalBanArguments();
      assertEquals(1, proxy.getCommands().getCommands().get("ban").getNumArgsRequired());
      assertEquals(3, server.getCommands().getCommands().get("ban").getNumArgsRequired());
    } finally {
      proxy.shutdown();
      server.shutdown();
    }
  }

  @Test
  void administratorPermissionExposesCommands() {
    MockPluginPlatform host =
        new MockPluginPlatform() {
          @Override
          public boolean hasPermission(String username, String permission) {
            return permission.equals("tebex.admin");
          }
        };
    try {
      assertEquals(
          host.getCommands().getCommands().size(),
          host.getCommands().getAllowedCommands("Admin").size());
    } finally {
      host.shutdown();
    }
  }

  @Test
  void deniedPermissionAndMissingArgumentsDoNotRunHandler() {
    MockPluginPlatform host = new MockPluginPlatform();
    try {
      AtomicReference<CompletableFuture<String[]>> response = new AtomicReference<>();
      assertFalse(
          host.getCommands()
              .process(
                  Context.from(
                      false,
                      "Steve",
                      null,
                      "tebex secret",
                      "",
                      null,
                      new String[] {"secret", "key"}),
                  response::set));
      assertTrue(response.get().join()[0].contains("permission"));
      assertFalse(
          host.getCommands()
              .process(
                  Context.from(true, "", null, "tebex secret", "", null, new String[] {"secret"}),
                  response::set));
      assertTrue(response.get().join()[0].contains("Usage"));
    } finally {
      host.shutdown();
    }
  }
}
