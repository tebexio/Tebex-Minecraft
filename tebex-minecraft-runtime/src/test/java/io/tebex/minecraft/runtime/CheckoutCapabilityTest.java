package io.tebex.minecraft.runtime;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import io.tebex.http.PluginApi;
import io.tebex.model.QueuedPlayer;
import io.tebex.model.ServerInformation;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import org.junit.jupiter.api.Test;

class CheckoutCapabilityTest {
  @Test
  void javaCheckoutWithoutFloodgateDoesNotInventAnXuidWarning() {
    Host host = new Host();
    try {
      host.sendCheckoutLink("JustCain", "https://example.test/checkout");
      assertEquals(1, host.messages.size());
      assertTrue(host.messages.get(0).contains("https://example.test/checkout"));
      assertTrue(host.warnings.isEmpty());
      // The checkout probe must not consume the genuine missing-XUID warning's once flag.
      host.setStoreInfo(
          new Gson()
              .fromJson(
                  "{\"account\":{\"game_type\":\"Minecraft (Offline/Geyser)\"}}",
                  ServerInformation.class));
      QueuedPlayer bedrock =
          new Gson()
              .fromJson("{\"name\":\"Bedrock\",\"xuid\":\"2533274913322943\"}", QueuedPlayer.class);
      assertEquals("2533274913322943", host.resolveCommandPlayerId(bedrock));
      assertEquals(1, host.warnings.size());
      host.resolveCommandPlayerId(bedrock);
      assertEquals(1, host.warnings.size());
    } finally {
      host.shutdown();
    }
  }

  private static final class Host extends MockPluginPlatform {
    final List<String> messages = new ArrayList<>(), warnings = new ArrayList<>();

    Host() {
      this(createApi());
    }

    private Host(PluginApi api) {
      super(key -> new SdkSession(key, api), System::currentTimeMillis);
      connect("fixture", false).join();
    }

    private static PluginApi createApi() {
      PluginApi api = org.mockito.Mockito.mock(PluginApi.class);
      org.mockito.Mockito.when(api.getServerInformation("fixture"))
          .thenReturn(
              CompletableFuture.completedFuture(
                  new Gson()
                      .fromJson(
                          "{\"account\":{\"id\":1,\"name\":\"Shop\",\"game_type\":\"Minecraft: Java Edition\",\"currency\":{\"iso_4217\":\"USD\"}},\"server\":{\"id\":1,\"name\":\"Test\"},\"public_token\":\"public\"}",
                          ServerInformation.class)));
      return api;
    }

    @Override
    public UUID getPlayerUniqueId(String name) {
      return UUID.fromString("bdd4b158-7983-458c-ad6e-cc1a0e20624e");
    }

    @Override
    public void sendPlayerMessage(String name, String message) {
      messages.add(message);
    }

    @Override
    public void log(Level level, String message) {
      if (level == Level.WARNING) warnings.add(message);
    }
  }
}
