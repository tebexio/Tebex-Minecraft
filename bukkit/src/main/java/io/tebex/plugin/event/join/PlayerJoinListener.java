package io.tebex.plugin.event.join;

import io.tebex.plugin.BukkitPluginPlatform;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener extends JoinListenerBase {

    public PlayerJoinListener(BukkitPluginPlatform platform) {
        super(platform);
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        handlePlayerJoin(event.getPlayer());
    }
}
