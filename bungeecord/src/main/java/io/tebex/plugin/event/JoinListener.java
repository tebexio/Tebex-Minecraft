package io.tebex.plugin.event;

import io.tebex.plugin.BungeePlatform;
import java.util.UUID;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class JoinListener implements Listener {
  private final BungeePlatform platform;

  public JoinListener(BungeePlatform platform) {
    this.platform = platform;
  }

  @EventHandler
  public void onPlayerConnect(LoginEvent event) {
    UUID uuid = event.getConnection().getUniqueId();
    String name = event.getConnection().getName();

    platform
        .runtime()
        .onPlayerJoin(name, uuid, event.getConnection().getAddress().getAddress().getHostAddress());
  }
}
