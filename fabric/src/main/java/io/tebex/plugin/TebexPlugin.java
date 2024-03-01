package io.tebex.plugin;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.analytics.AnalyticsService;
import io.tebex.plugin.store.StoreService;
import io.tebex.plugin.store.command.CommandManager;
import io.tebex.plugin.store.listener.JoinListener;
import io.tebex.plugin.util.Multithreading;
import io.tebex.sdk.Tebex;
import io.tebex.sdk.platform.Platform;
import io.tebex.sdk.platform.PlatformTelemetry;
import io.tebex.sdk.platform.PlatformType;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import io.tebex.sdk.store.SDK;
import io.tebex.sdk.store.obj.Category;
import io.tebex.sdk.store.obj.ServerEvent;
import io.tebex.sdk.store.placeholder.PlaceholderManager;
import io.tebex.sdk.store.response.ServerInformation;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.LiteralText;
import net.minecraft.util.collection.DefaultedList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Fabric plugin for Tebex.
 */
public class TebexPlugin implements Platform, DedicatedServerModInitializer {
    // Fabric Related
    private static final String MOD_ID = "tebex";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    private final String MOD_VERSION = "@VERSION@";
    private final File MOD_PATH = new File("./mods/" + MOD_ID);
    private ServerPlatformConfig config;
    private YamlDocument configYaml;
    private StoreService storeService;
    private AnalyticsService analyticsService;
    private MinecraftServer server;

    /**
     * Starts the Fabric platform.
     */
    @Override
    public void onInitializeServer() {
        try {
            // Load the platform config file.
            configYaml = initPlatformConfig();
            config = loadServerPlatformConfig(configYaml);
        } catch (IOException e) {
            log(Level.WARNING, "Failed to load config: " + e.getMessage());
            return;
        }

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            this.server = server;
            onEnable();
        });

        // Initialise Managers.
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> new CommandManager(this).register(dispatcher));

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> Multithreading.shutdown());
    }

    private void onEnable() {
        // Bind SDK.
        Tebex.init(this);

        boolean storeSetup = getPlatformConfig().getStoreSecretKey() != null && !getPlatformConfig().getStoreSecretKey().isEmpty();
        boolean analyticsSetup = getPlatformConfig().getAnalyticsSecretKey() != null && !getPlatformConfig().getAnalyticsSecretKey().isEmpty();

        String messagePart = !storeSetup && !analyticsSetup ? "Store and Analytics" : !storeSetup ? "Store" : !analyticsSetup ? "Analytics" : "";

        info("Thanks for installing Tebex v" + MOD_VERSION + " for Fabric.");

        if (!storeSetup || !analyticsSetup) {
            warning("It seems that you're using a fresh install, or haven't configured your " + messagePart + " secret keys yet!");
            warning(" ");

            if (!storeSetup) {
                warning("To setup your Tebex Store, run 'tebex secret <key>' in the console.");
            }
            if (!analyticsSetup) {
                warning("To setup your Tebex Analytics, run 'analytics secret <key>' in the console.");
            }

            warning(" ");
            warning("We recommend running these commands from the console to avoid accidentally sharing your secret keys in chat.");
        }

        // Initialise Managers.
        storeService = new StoreService(this);
        storeService.init();

        if (storeSetup) {
            storeService.connect();
        }

        analyticsService = new AnalyticsService(this);
        analyticsService.init();

        if (analyticsSetup) {
            analyticsService.connect();
        }
    }

    @Override
    public PlatformType getType() {
        return PlatformType.FABRIC;
    }

    @Override
    public SDK getStoreSDK() {
        return storeService.getSdk();
    }

    @Override
    public File getDirectory() {
        return MOD_PATH;
    }

    @Override
    public boolean isStoreSetup() {
        return storeService.isSetup();
    }

    @Override
    public boolean isAnalyticsSetup() {
        return analyticsService.isSetup();
    }

    @Override
    public boolean isOnlineMode() {
        return getPlatformConfig().isProxyMode() || server.isOnlineMode();
    }

    @Override
    public void halt() {
        storeService.setSetup(false);
    }

    @Override
    public PlaceholderManager getPlaceholderManager() {
        return storeService.getPlaceholderManager();
    }

    @Override
    public Map<Object, Integer> getQueuedPlayers() {
        return storeService.getQueuedPlayers();
    }

    @Override
    public void dispatchCommand(String command) {
        server.getCommandManager().execute(server.getCommandSource(), command);
    }

    @Override
    public void executeAsync(Runnable runnable) {
        Multithreading.runAsync(runnable);
    }

    @Override
    public void executeAsyncLater(Runnable runnable, long time, TimeUnit unit) {
        Multithreading.executeAsyncLater(runnable, time, unit);
    }

    @Override
    public void executeBlocking(Runnable runnable) {
        try {
            Multithreading.executeBlocking(runnable);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void executeBlockingLater(Runnable runnable, long time, TimeUnit unit) {
        try {
            Multithreading.executeBlockingLater(runnable, time, unit);
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isPlayerOnline(Object player) {
        return getPlayer(player).isPresent();
    }

    private Optional<ServerPlayerEntity> getPlayer(Object player) {
        if (player == null) return Optional.empty();

        if (isOnlineMode() && !isGeyser() && player instanceof UUID) {
            return Optional.ofNullable(server.getPlayerManager().getPlayer((UUID) player));
        }

        return Optional.ofNullable(server.getPlayerManager().getPlayer((String) player));
    }

    @Override
    public int getFreeSlots(Object playerId) {
        ServerPlayerEntity player = getPlayer(playerId).orElse(null);
        if (player == null) return -1;

        DefaultedList<ItemStack> inv = player.inventory.main;
        return (int) inv.stream()
                .filter(obj -> obj == null || obj.isEmpty())
                .count();
    }

    @Override
    public String getVersion() {
        return MOD_VERSION;
    }

    @Override
    public String getStoreType() {
        return storeService.getStoreInformation() == null ? "" : storeService.getStoreInformation().getStore().getGameType();
    }

    @Override
    public void log(Level level, String message) {
        if (level == Level.INFO) {
            LOGGER.info(message);
        } else if (level == Level.WARNING) {
            LOGGER.warn(message);
        } else if (level == Level.SEVERE) {
            LOGGER.error(message);
        } else {
            LOGGER.info(message);
        }
    }

    public List<Category> getStoreCategories() {
        return storeService.getStoreCategories();
    }

    @Override
    public void setStoreCategories(List<Category> categories) {
        storeService.setStoreCategories(categories);
    }

    @Override
    public ServerPlatformConfig getPlatformConfig() {
        return config;
    }

    @Override
    public PlatformTelemetry getTelemetry() {
        String serverVersion = server.getVersion();

        Pattern pattern = Pattern.compile("MC: (\\d+\\.\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(serverVersion);
        if (matcher.find()) {
            serverVersion = matcher.group(1);
        }

        return new PlatformTelemetry(
                getVersion(),
                server.getName(),
                serverVersion,
                System.getProperty("java.version"),
                System.getProperty("os.arch"),
                server.isOnlineMode()
        );
    }

    @Override
    public String getServerIp() {
        return this.server.getServerIp();
    }

    public ServerInformation getStoreInformation() {
        return storeService.getStoreInformation();
    }

    @Override
    public void setStoreInformation(ServerInformation info) {
        storeService.setStoreInformation(info);
    }

    public List<ServerEvent> getServerEvents() {
        return storeService.getServerEvents();
    }

    public StoreService getStoreManager() {
        return storeService;
    }

    public AnalyticsService getAnalyticsManager() {
        return analyticsService;
    }

    public void sendMessage(ServerCommandSource source, String message) {
        LiteralText text = new LiteralText("§b[Tebex] §7" + message.replace("&", "§"));

        // Sending formatted message to the player
        source.sendFeedback(text, false);
    }

    public void sendMessage(ServerPlayerEntity source, String message) {
        LiteralText text = new LiteralText("§b[Tebex] §7" + message.replace("&", "§"));

        // Sending formatted message to the player
        source.sendMessage(text, false);
    }

    public MinecraftServer getServer() {
        return server;
    }
}
