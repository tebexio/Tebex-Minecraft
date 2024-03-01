package io.tebex.plugin.analytics.manager;

import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.util.Multithreading;
import io.tebex.sdk.exception.NotFoundException;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class HeartbeatManager {
    private final TebexPlugin platform;
    private ScheduledFuture<?> task;
    private boolean enabled;

    public HeartbeatManager(TebexPlugin platform) {
        this.platform = platform;
        enabled = true;
    }

    public void start() {
        Multithreading.schedule(() -> {
            if(! enabled) return;
            int playerCount = platform.getServer().getCurrentPlayerCount();

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
        }, 0, 1, TimeUnit.MINUTES);
    }

    public void stop() {
        if (task == null) return;
        enabled = false;
    }
}
