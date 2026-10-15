package io.tebex.plugin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.junit.jupiter.api.Test;

class PlayerLookupTest {
  @Test
  void acceptsUuidInTheProxyNameBasedAuthenticationMode() {
    TebexBungeePlugin plugin = mock(TebexBungeePlugin.class);
    ProxyServer proxy = mock(ProxyServer.class);
    ProxiedPlayer player = mock(ProxiedPlayer.class);
    UUID id = UUID.randomUUID();
    when(plugin.getProxy()).thenReturn(proxy);
    when(proxy.getPlayer(id)).thenReturn(player);
    when(proxy.getPlayer("JustCain")).thenReturn(player);
    BungeePlatform host = new BungeePlatform(plugin);
    try {
      assertSame(player, host.getPlayer(id));
      assertSame(player, host.getPlayer("JustCain"));
      assertNull(host.getPlayer(42));
      assertNull(host.getPlayer("Missing"));
    } finally {
      host.runtime().shutdown();
    }
  }
}
