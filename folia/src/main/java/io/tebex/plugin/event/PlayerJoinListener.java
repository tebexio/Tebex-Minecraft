package io.tebex.plugin.event;

import io.tebex.plugin.FoliaPluginPlatform;
import io.tebex.sdk.obj.QueuedPlayer;
import io.tebex.sdk.obj.ServerEvent;
import io.tebex.sdk.obj.EnumServerEventType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {
    private final FoliaPluginPlatform platform;

    public PlayerJoinListener(FoliaPluginPlatform platform) {
        this.platform = platform;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Object playerId = platform.getPlayerId(player.getName(), player.getUniqueId());
        platform.getJoinEvents().add(new ServerEvent(player.getUniqueId().toString(), player.getName(), player.getAddress().getAddress().getHostAddress(), EnumServerEventType.JOIN));

        Integer queuedId = platform.getQueuedPlayers().get(playerId);
        if (queuedId == null) {
            return;
        }

        platform.handleOnlineCommands(new QueuedPlayer(queuedId, player.getName(), player.getUniqueId().toString()));
    }
}
