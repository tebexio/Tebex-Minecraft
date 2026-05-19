package io.tebex.plugin.event.join;

import io.tebex.plugin.BukkitPluginPlatform;
import net.kyori.adventure.audience.Audience;
import net.william278.husksync.event.BukkitSyncCompleteEvent;
import net.william278.husksync.user.OnlineUser;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

public class HuskSyncPlayerJoinListener extends PlayerJoinListener {

    public HuskSyncPlayerJoinListener(BukkitPluginPlatform platform) {
        super(platform);
    }

    @EventHandler
    private void onBukkitSyncComplete(BukkitSyncCompleteEvent event) {
        OnlineUser user = event.getUser();

        if (user == null) {
            return;
        }

        Audience audience = user.getAudience();
        if (audience == null) {
            return;
        }

        if (audience instanceof Player) {
            Player player = (Player) audience;
            handlePlayerJoin(player);
        }
    }
}
