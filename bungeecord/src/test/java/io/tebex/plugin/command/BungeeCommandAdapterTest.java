package io.tebex.plugin.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.tebex.plugin.BungeePlatform;
import io.tebex.plugin.TebexBungeePlugin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.junit.jupiter.api.Test;

class BungeeCommandAdapterTest {
  @Test
  void tabCompletionRespectsProxyCommandSurfaceAndPlayerPermissions() {
    TebexBungeePlugin plugin = mock(TebexBungeePlugin.class);
    BungeePlatform platform = new BungeePlatform(plugin);
    try {
      platform
          .runtime()
          .getCommands()
          .setRestrictedToCommands("help", "forcecheck", "reload", "secret", "debug");
      TebexCommandExecutor command = new TebexCommandExecutor(platform);
      ProxiedPlayer player = mock(ProxiedPlayer.class);
      when(player.hasPermission("tebex.help")).thenReturn(true);
      when(player.hasPermission("tebex.debug")).thenReturn(true);

      List<String> suggestions = new ArrayList<>();
      command.onTabComplete(player, new String[] {""}).forEach(suggestions::add);

      assertEquals(Arrays.asList("help", "debug"), suggestions);
    } finally {
      platform.runtime().shutdown();
    }
  }
}
