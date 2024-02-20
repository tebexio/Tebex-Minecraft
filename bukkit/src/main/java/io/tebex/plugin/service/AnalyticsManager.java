package io.tebex.plugin.service;

import com.google.common.collect.Maps;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.event.PlayerJoinListener;
import io.tebex.plugin.event.PlayerQuitListener;
import io.tebex.plugin.manager.AnalyticsCommandManager;
import io.tebex.sdk.AnalyticsSDK;
import io.tebex.sdk.analytics.obj.AnalysePlayer;
import io.tebex.sdk.exception.NotFoundException;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public class AnalyticsManager implements ServiceManager {
    private final TebexPlugin platform;
    private AnalyticsSDK sdk;
    private boolean setup;
    private ConcurrentMap<UUID, AnalysePlayer> players;

    public AnalyticsManager(TebexPlugin platform) {
        this.platform = platform;
        this.players = Maps.newConcurrentMap();
    }

    @Override
    public void load() {
        sdk = new AnalyticsSDK(platform, platform.getPlatformConfig().getSecretKey());

        new AnalyticsCommandManager(platform).register();

        // Register events.
        platform.registerEvents(new PlayerJoinListener(platform));
        platform.registerEvents(new PlayerQuitListener(platform));
    }

    @Override
    public void connect() {
        sdk.getServerInformation().thenAccept(serverInformation -> {
            platform.info(String.format("Connected to %s on Tebex Analytics.", serverInformation.getName()));
            this.setup = true;
        }).exceptionally(ex -> {
            Throwable cause = ex.getCause();
            this.setup = false;

            if (cause instanceof NotFoundException) {
                platform.warning("Failed to connect to Tebex Analytics. Please double-check your server key or run the setup command again.");
                platform.halt();
            } else {
                platform.warning("Failed to get analytics information: " + cause.getMessage());
                cause.printStackTrace();
            }

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

    public ConcurrentMap<UUID, AnalysePlayer> getPlayers() {
        return players;
    }

    public AnalysePlayer getPlayer(UUID uuid) {
        return players.get(uuid);
    }
}
