package io.tebex.plugin.store;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.ServiceManager;
import io.tebex.plugin.store.gui.BuyGUI;
import io.tebex.plugin.store.listener.JoinListener;
import io.tebex.plugin.util.Multithreading;
import io.tebex.sdk.exception.NotFoundException;
import io.tebex.sdk.store.SDK;
import io.tebex.sdk.store.obj.Category;
import io.tebex.sdk.store.obj.ServerEvent;
import io.tebex.sdk.store.placeholder.PlaceholderManager;
import io.tebex.sdk.store.response.ServerInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class StoreService implements ServiceManager {
    private final TebexPlugin platform;
    private final PlaceholderManager placeholderManager;
    private final Map<Object, Integer> queuedPlayers;
    private ServerInformation storeInformation;
    private List<Category> storeCategories;
    private final List<ServerEvent> serverEvents;
    public BuyGUI buyGUI;
    private SDK sdk;
    private boolean setup;

    public StoreService(TebexPlugin platform) {
        this.platform = platform;

        sdk = new SDK(platform, platform.getPlatformConfig().getStoreSecretKey());
        placeholderManager = new PlaceholderManager();
        queuedPlayers = Maps.newConcurrentMap();
        storeCategories = new ArrayList<>();
        serverEvents = new ArrayList<>();
        buyGUI = new BuyGUI(platform);
    }

    @Override
    public void init() {
        // Register events.
        new JoinListener(platform);
    }

    @Override
    public void connect() {
        sdk.getServerInformation().thenAccept(serverInformation -> {
            ServerInformation.Server server = serverInformation.getServer();
            ServerInformation.Store store = serverInformation.getStore();

            platform.info(String.format("Connected to %s - %s on Tebex Creator Panel.", server.getName(), store.getGameType()));

            setSetup(true);
            platform.performCheck();
            sdk.sendTelemetry();

            Multithreading.executeAsync(platform::refreshListings, 0, 5, TimeUnit.HOURS);
            Multithreading.executeAsync(() -> {
                List<ServerEvent> runEvents = Lists.newArrayList(serverEvents.subList(0, Math.min(serverEvents.size(), 750)));
                if (runEvents.isEmpty()) return;
                if (!setup) return;

                sdk.sendEvents(runEvents)
                        .thenAccept(aVoid -> {
                            serverEvents.removeAll(runEvents);
                            platform.debug("Successfully sent analytics.");
                        })
                        .exceptionally(throwable -> {
                            platform.debug("Failed to send analytics: " + throwable.getMessage());
                            return null;
                        });
            }, 0, 1, TimeUnit.MINUTES);
        }).exceptionally(ex -> {
            Throwable cause = ex.getCause();
            setSetup(false);

            if (cause instanceof NotFoundException) {
                platform.warning("Failed to connect to Tebex Webstore. Please double-check your server key or run the setup command again.");
                platform.halt();
                return null;
            }

            platform.warning("Failed to get server information: " + cause.getMessage());
            cause.printStackTrace();

            return null;
        });
    }

    public SDK getSdk() {
        return sdk;
    }

    public BuyGUI getBuyGui() {
        return buyGUI;
    }

    public Map<Object, Integer> getQueuedPlayers() {
        return queuedPlayers;
    }

    public List<ServerEvent> getServerEvents() {
        return serverEvents;
    }

    public List<Category> getStoreCategories() {
        return storeCategories;
    }

    public void setStoreCategories(List<Category> storeCategories) {
        this.storeCategories = storeCategories;
    }

    public PlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }

    public ServerInformation getStoreInformation() {
        return storeInformation;
    }

    public void setStoreInformation(ServerInformation storeInformation) {
        this.storeInformation = storeInformation;
    }

    public void setBuyGui(BuyGUI buyGUI) {
        this.buyGUI = buyGUI;
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
