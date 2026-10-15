package io.tebex.plugin.event;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import io.tebex.plugin.VelocityPlatform;

public class JoinListener {
  private final VelocityPlatform platform;

  public JoinListener(VelocityPlatform platform) {
    this.platform = platform;
  }

  @Subscribe
  public void onPlayerConnect(LoginEvent event) {
    Player player = event.getPlayer();

    platform
        .runtime()
        .onPlayerJoin(
            player.getUsername(),
            player.getUniqueId(),
            player.getRemoteAddress().getAddress().getHostAddress());
  }
}
