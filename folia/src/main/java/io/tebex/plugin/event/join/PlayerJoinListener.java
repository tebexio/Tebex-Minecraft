package io.tebex.plugin.event.join;

import io.tebex.plugin.FoliaPluginPlatform;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener extends JoinListenerBase {

    public PlayerJoinListener(FoliaPluginPlatform platform) {
        super(platform);
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        handlePlayerJoin(event.getPlayer());
    }
}
