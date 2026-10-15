package io.tebex.plugin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class PlayerLookupTest {
  @Test
  void unwrapsPlayerAndUsesUuidOverloadWithoutDependingOnAuthenticationMode() {
    ProxyServer proxy = mock(ProxyServer.class);
    Player player = mock(Player.class);
    UUID id = UUID.randomUUID();
    when(proxy.getPlayer(id)).thenReturn(Optional.of(player));
    when(proxy.getPlayer("JustCain")).thenReturn(Optional.of(player));
    when(proxy.getPlayer("Missing")).thenReturn(Optional.empty());
    TebexVelocityPlugin plugin = mock(TebexVelocityPlugin.class);
    VelocityPlatform platform =
        new VelocityPlatform(plugin, proxy, mock(Logger.class), Paths.get("fixture"));
    try {
      assertSame(player, platform.getPlayer(id));
      assertSame(player, platform.getPlayer("JustCain"));
      assertNull(platform.getPlayer("Missing"));
      assertNull(platform.getPlayer(42));
      assertNull(platform.getPlayer(null));
      assertFalse(platform.isPlayerOnline("Missing"));
    } finally {
      platform.runtime().shutdown();
    }
  }
}
