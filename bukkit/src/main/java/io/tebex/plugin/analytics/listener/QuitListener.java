package io.tebex.plugin.analytics.listener;

import com.google.common.collect.Maps;
import io.tebex.plugin.TebexPlugin;
import io.tebex.sdk.analytics.obj.Event;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Date;

public class QuitListener implements Listener {
    private final TebexPlugin platform;

    public QuitListener(TebexPlugin platform) {
        this.platform = platform;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player bukkitPlayer = event.getPlayer();

        Event playerEvent = new Event("player:quit", "Analyse", new Date(), bukkitPlayer.getUniqueId());
        platform.getAnalyticsManager().getEvents().add(playerEvent);

        platform.debug("Preparing to track " + bukkitPlayer.getName() + "..");
    }
}
