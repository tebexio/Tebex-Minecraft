package io.tebex.plugin.store;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.store.command.BuyCommand;
import io.tebex.plugin.store.command.CommandManager;
import io.tebex.plugin.store.gui.BuyGUI;
import io.tebex.plugin.store.placeholder.BukkitNamePlaceholder;
import io.tebex.plugin.obj.ServiceManager;
import io.tebex.sdk.StoreSDK;
import io.tebex.sdk.exception.NotFoundException;
import io.tebex.sdk.store.obj.Category;
import io.tebex.sdk.store.obj.ServerEvent;
import io.tebex.sdk.store.placeholder.PlaceholderManager;
import io.tebex.sdk.store.placeholder.defaults.UuidPlaceholder;
import io.tebex.sdk.store.response.ServerInformation;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StoreManager implements ServiceManager {
    private final TebexPlugin platform;
    private final PlaceholderManager placeholderManager;
    private final Map<Object, Integer> queuedPlayers;
    private ServerInformation storeInformation;
    private List<Category> storeCategories;
    private final List<ServerEvent> serverEvents;
    public BuyGUI buyGUI;
    private StoreSDK sdk;
    private boolean setup;

    public StoreManager(TebexPlugin platform) {
        this.platform = platform;

        sdk = new StoreSDK(platform, platform.getPlatformConfig().getStoreSecretKey());
        placeholderManager = new PlaceholderManager();
        queuedPlayers = Maps.newConcurrentMap();
        storeCategories = new ArrayList<>();
        serverEvents = new ArrayList<>();
        buyGUI = new BuyGUI(platform);
    }

    @Override
    public void init() {
        new CommandManager(platform).register();

        // Register the custom /buy command
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

            commandMap.register(platform.getPlatformConfig().getBuyCommandName(), new BuyCommand(platform.getPlatformConfig().getBuyCommandName(), platform));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to get the CommandMap", e);
        }

        placeholderManager.register(new BukkitNamePlaceholder(placeholderManager));
        placeholderManager.register(new UuidPlaceholder(placeholderManager));
    }

    @Override
    public void connect() {
        getSdk().getServerInformation().thenAccept(serverInformation -> {
            ServerInformation.Server server = serverInformation.getServer();
            ServerInformation.Store store = serverInformation.getStore();

            platform.info(String.format("Connected to the %s - %s store.", server.getName(), store.getGameType()));

            setSetup(true);
            platform.performCheck();
            sdk.sendTelemetry();

            platform.getServer().getScheduler().runTaskTimerAsynchronously(platform, platform::refreshListings, 0, 20 * 60 * 5);
            platform.getServer().getScheduler().runTaskTimerAsynchronously(platform, () -> {
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
            }, 0, 20 * 60);
        }).exceptionally(ex -> {
            Throwable cause = ex.getCause();
            setSetup(false);

            if (cause instanceof NotFoundException) {
                platform.warning("Failed to connect. Please double-check your server key or run the setup command again.");
                platform.halt();
                return null;
            }

            platform.warning("Failed to get server information: " + cause.getMessage());
            cause.printStackTrace();

            return null;
        });
    }

    public StoreSDK getSdk() {
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
