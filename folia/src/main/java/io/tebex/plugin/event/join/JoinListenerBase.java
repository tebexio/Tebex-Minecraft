package io.tebex.plugin.event.join;

import io.tebex.plugin.FoliaPluginPlatform;
import io.tebex.sdk.obj.QueuedPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public abstract class JoinListenerBase implements Listener {

    private final FoliaPluginPlatform platform;

    public JoinListenerBase(FoliaPluginPlatform platform) {
        this.platform = platform;
    }

    protected void handlePlayerJoin(Player player) {
        Object playerId = platform.getPlayerId(player.getName(), player.getUniqueId());
        platform.createJoinEvent(player.getUniqueId().toString(), player.getName(), player.getAddress().getAddress().getHostAddress());

        if(! platform.getQueuedPlayers().containsKey(playerId)) {
            return;
        }

        platform.handleOnlineCommands(new QueuedPlayer(platform.getQueuedPlayers().get(playerId), player.getName(), player.getUniqueId().toString()));
    }
}
