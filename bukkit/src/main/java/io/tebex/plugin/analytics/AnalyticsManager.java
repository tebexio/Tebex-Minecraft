package io.tebex.plugin.analytics;

import com.google.common.collect.Maps;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.analytics.listener.JoinListener;
import io.tebex.plugin.analytics.listener.QuitListener;
import io.tebex.plugin.analytics.command.CommandManager;
import io.tebex.plugin.analytics.manager.HeartbeatManager;
import io.tebex.plugin.obj.ServiceManager;
import io.tebex.sdk.AnalyticsSDK;
import io.tebex.sdk.analytics.obj.AnalysePlayer;
import io.tebex.sdk.exception.NotFoundException;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public class AnalyticsManager implements ServiceManager {
    private final TebexPlugin platform;
    private final HeartbeatManager heartbeatManager;
    private AnalyticsSDK sdk;
    private boolean setup;
    private ConcurrentMap<UUID, AnalysePlayer> players;

    public AnalyticsManager(TebexPlugin platform) {
        this.platform = platform;
        this.players = Maps.newConcurrentMap();
        this.heartbeatManager = new HeartbeatManager(platform);
    }

    @Override
    public void load() {
        sdk = new AnalyticsSDK(platform, platform.getPlatformConfig().getAnalyticsSecretKey());

        new CommandManager(platform).register();

        // Register events.
        platform.registerEvents(new JoinListener(platform));
        platform.registerEvents(new QuitListener(platform));
    }

    @Override
    public void connect() {
        sdk.getServerInformation().thenAccept(serverInformation -> {
            platform.info(String.format("Connected to the %s analytics.", serverInformation.getName()));
            this.setup = true;
            this.heartbeatManager.start();
        }).exceptionally(ex -> {
            Throwable cause = ex.getCause();
            this.setup = false;
            this.heartbeatManager.stop();

            if (cause instanceof NotFoundException) {
                platform.warning("Failed to connect to Tebex Analytics. Please double-check your server key or run the setup command again.");
                platform.halt();
                return null;
            }

            platform.warning("Failed to get analytics information: " + cause.getMessage());
            cause.printStackTrace();
            return null;
        });
    }

    public AnalyticsSDK getSdk() {
        return sdk;
    }

    @Override
    public boolean isSetup() {
        return setup;
    }

    @Override
    public void setSetup(boolean setup) {
        this.setup = setup;
    }

    public HeartbeatManager getHeartbeatManager() {
        return heartbeatManager;
    }

    public ConcurrentMap<UUID, AnalysePlayer> getPlayers() {
        return players;
    }

    public AnalysePlayer getPlayer(UUID uuid) {
        return players.get(uuid);
    }

    public void removePlayer(UUID uuid) {
        players.remove(uuid);
    }
}
