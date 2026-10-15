package io.tebex.minecraft.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayerResolverTest {
  @Test
  void selectsStableJavaOrNameIdentityForTheStoreMode() {
    MockPluginPlatform host = new MockPluginPlatform();
    try {
      PlayerResolver resolver = new PlayerResolver(host, ignored -> {}, (message, solution) -> {});
      UUID uuid = UUID.fromString("bdd4b158-7983-458c-ad6e-cc1a0e20624e");

      assertEquals(uuid, resolver.playerId("Steve", uuid, true, false));
      assertEquals("Steve", resolver.playerId("Steve", uuid, false, false));
      assertEquals("Steve", resolver.playerId("Steve", uuid, true, true));
      assertEquals("", resolver.playerId(null, null, true, false));
    } finally {
      host.shutdown();
    }
  }
}
