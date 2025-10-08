package io.tebex.plugin.event;

import io.tebex.plugin.BukkitPluginPlatform;
import io.tebex.sdk.obj.QueuedPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final BukkitPluginPlatform platform;

    public PlayerJoinListener(BukkitPluginPlatform platform) {
        this.platform = platform;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Object playerId = platform.getPlayerId(player.getName(), player.getUniqueId());

        Integer queuedId = platform.getQueuedPlayers().get(playerId);
        if (queuedId == null) {
            return;
        }

        platform.handleOnlineCommands(new QueuedPlayer(queuedId, player.getName(), player.getUniqueId().toString()));
    }
}
