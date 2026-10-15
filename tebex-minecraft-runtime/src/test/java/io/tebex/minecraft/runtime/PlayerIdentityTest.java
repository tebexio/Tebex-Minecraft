package io.tebex.minecraft.runtime;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import io.tebex.minecraft.util.PlayerIdentity;
import io.tebex.model.*;
import org.junit.jupiter.api.Test;

class PlayerIdentityTest {
  @Test
  void canonicalizesJavaUuidAndDoesNotReplaceUnknownTags() {
    QueuedCommand command =
        new Gson()
            .fromJson(
                "{\"command\":\"give {UUID} {username} {unknown}\",\"player\":{\"name\":\"A$B\",\"uuid\":\"123456781234123412341234567890ab\"}}",
                QueuedCommand.class);
    MockPluginPlatform host = new MockPluginPlatform();
    try {
      assertEquals(
          "give 12345678-1234-1234-1234-1234567890ab A$B {unknown}",
          PlayerIdentity.command(command, host.getRuntime()));
    } finally {
      host.shutdown();
    }
  }

  @Test
  void missingOrMalformedUuidFallsBackToXuidThenName() {
    QueuedPlayer player =
        new Gson()
            .fromJson(
                "{\"name\":\"Steve\",\"uuid\":\"not-a-uuid\",\"xuid\":\"2533274913322943\"}",
                QueuedPlayer.class);
    assertFalse(PlayerIdentity.hasUuid(player));
    assertEquals("2533274913322943", PlayerIdentity.defaultIdentifier(player));
    assertEquals(Long.valueOf(2533274913322943L), PlayerIdentity.xuid(player));
    QueuedPlayer offline =
        new Gson()
            .fromJson(
                "{\"name\":\"Steve\",\"uuid\":\"00000000000000000000000000000000\"}",
                QueuedPlayer.class);
    assertEquals("Steve", PlayerIdentity.defaultIdentifier(offline));
    assertNull(PlayerIdentity.uuid("invalid"));
  }

  @Test
  void unavailableFloodgateRetainsXuidAndDoesNotCrash() {
    MockPluginPlatform host =
        new MockPluginPlatform() {
          @Override
          public String resolveCommandPlayerId(QueuedPlayer p) {
            return super.resolveCommandPlayerId(p);
          }
        };
    try {
      host.setStoreInfo(
          new Gson()
              .fromJson(
                  "{\"account\":{\"game_type\":\"Minecraft (Offline/Geyser)\"}}",
                  ServerInformation.class));
      QueuedPlayer player =
          new Gson()
              .fromJson("{\"name\":\"Steve\",\"xuid\":\"2533274913322943\"}", QueuedPlayer.class);
      assertEquals("2533274913322943", host.resolveCommandPlayerId(player));
    } finally {
      host.shutdown();
    }
  }
}
