package io.tebex.plugin.analytics.manager;

import io.tebex.plugin.TebexPlugin;
import io.tebex.sdk.analytics.obj.Event;
import org.bukkit.Bukkit;
import space.arim.morepaperlib.scheduling.ScheduledTask;

import java.util.Date;

public class HeartbeatManager {
    private final TebexPlugin platform;
    private ScheduledTask task;

    public HeartbeatManager(TebexPlugin platform) {
        this.platform = platform;
    }

    public void start() {
        task = platform.getScheduler().runAtFixedRate(() -> {
            if(! platform.getAnalyticsManager().isSetup()) return;
            if(platform.getPlayerCountService() == null) return;

            int playerCount = platform.getPlayerCountService().getPlayerCount();

            if(playerCount == 0) {
                platform.debug("Not sending heartbeat as there are no players online.");
                return;
            }

            Event playerEvent = new Event("server:heartbeat", "Analyse", new Date()).withMetadata("players", playerCount);
            platform.getAnalyticsManager().addEvent(playerEvent);

            platform.debug("Successfully sent server heartbeat.");
        }, 1, 20 * 60);
    }

    public void stop() {
        if (task == null) return;

        task.cancel();
    }
}
