package io.tebex.plugin.analytics.manager;

import io.tebex.plugin.TebexPlugin;
import io.tebex.sdk.exception.NotFoundException;
import org.bukkit.Bukkit;
import space.arim.morepaperlib.scheduling.ScheduledTask;

import java.util.logging.Level;

public class HeartbeatManager {
    private final TebexPlugin platform;
    private ScheduledTask task;

    public HeartbeatManager(TebexPlugin platform) {
        this.platform = platform;
    }

    public void start() {
        task = platform.getScheduler().runAtFixedRate(() -> {
            int playerCount = Bukkit.getOnlinePlayers().size();

            if(playerCount == 0) {
                platform.debug("Not sending heartbeat as there are no players online.");
                return;
            }

            platform.getAnalyticsSDK().trackHeartbeat(playerCount).thenAccept(successful -> {
                if(! successful) {
                    platform.warning("Failed to send server heartbeat.");
                    return;
                }

                platform.debug("Successfully sent server heartbeat.");
            }).exceptionally(ex -> {
                Throwable cause = ex.getCause();
                platform.log(Level.WARNING, "Failed to track server heartbeat: " + cause.getMessage());

                if(cause instanceof NotFoundException) {
                    platform.halt();
                    return null;
                }

                cause.printStackTrace();
                return null;
            });
        }, 1, 20 * 60);
    }

    public void stop() {
        if (task == null) return;
        task.cancel();
    }
}
