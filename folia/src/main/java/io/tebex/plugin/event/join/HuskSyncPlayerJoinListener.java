package io.tebex.plugin.event.join;

import io.tebex.plugin.FoliaPluginPlatform;
import net.william278.husksync.event.BukkitSyncCompleteEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

public class HuskSyncPlayerJoinListener extends JoinListenerBase {

    public HuskSyncPlayerJoinListener(FoliaPluginPlatform platform) {
        super(platform);
    }

    @EventHandler
    private void onBukkitSyncCompleteEvent(BukkitSyncCompleteEvent event) {
        if (event.getUser().getAudience() instanceof Player player) {
            handlePlayerJoin(player);
        }
    }
}
