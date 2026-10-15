package io.tebex.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.tebex.plugin.command.CommandManager;
import io.tebex.plugin.command.TebexCommand;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class VelocityCommandAdapterTest {
  @Test
  void suggestionsRespectProxyCommandSurfaceAndPlayerPermissions() {
    ProxyServer proxy = mock(ProxyServer.class);
    VelocityPlatform platform =
        new VelocityPlatform(
            mock(TebexVelocityPlugin.class), proxy, mock(Logger.class), Paths.get("fixture"));
    try {
      platform
          .runtime()
          .getCommands()
          .setRestrictedToCommands("secret", "reload", "forcecheck", "help", "ban", "debug");
      TebexCommand command = new TebexCommand(new CommandManager(platform));
      Player player = mock(Player.class);
      when(player.hasPermission("tebex.help")).thenReturn(true);
      when(player.hasPermission("tebex.ban")).thenReturn(true);
      SimpleCommand.Invocation invocation = mock(SimpleCommand.Invocation.class);
      when(invocation.source()).thenReturn(player);
      when(invocation.arguments()).thenReturn(new String[] {""});

      List<String> suggestions = command.suggest(invocation);

      assertEquals(List.of("help", "ban"), suggestions);
    } finally {
      platform.runtime().shutdown();
    }
  }
}
