package io.tebex.plugin.event;

import io.tebex.plugin.FoliaPlatform;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
  private final FoliaPlatform platform;

  public PlayerJoinListener(FoliaPlatform platform) {
    this.platform = platform;
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    platform
        .runtime()
        .onPlayerJoin(
            player.getName(),
            player.getUniqueId(),
            player.getAddress() == null ? "" : player.getAddress().getAddress().getHostAddress());
  }
}
