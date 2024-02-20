package io.tebex.plugin.service;

import io.tebex.plugin.TebexPlugin;
import io.tebex.sdk.AnalyticsSDK;
import io.tebex.sdk.exception.NotFoundException;

public class AnalyticsManager implements ServiceManager {
    private final TebexPlugin platform;
    private AnalyticsSDK sdk;
    private boolean setup;

    public AnalyticsManager(TebexPlugin platform) {
        this.platform = platform;
    }

    @Override
    public void load() {
        sdk = new AnalyticsSDK(platform, platform.getPlatformConfig().getSecretKey());

//        new StoreCommandManager(platform).register();
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
}
