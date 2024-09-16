package io.tebex.plugin;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.command.BuyCommand;
import io.tebex.plugin.event.JoinListener;
import io.tebex.plugin.gui.BuyGUI;
import io.tebex.plugin.manager.CommandManager;
import io.tebex.plugin.placeholder.BukkitNamePlaceholder;
import io.tebex.sdk.SDK;
import io.tebex.sdk.Tebex;
import io.tebex.sdk.obj.Category;
import io.tebex.sdk.obj.ServerEvent;
import io.tebex.sdk.placeholder.PlaceholderManager;
import io.tebex.sdk.placeholder.defaults.UuidPlaceholder;
import io.tebex.sdk.platform.Platform;
import io.tebex.sdk.platform.PlatformTelemetry;
import io.tebex.sdk.platform.PlatformType;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import io.tebex.sdk.request.response.ServerInformation;
import io.tebex.sdk.util.CommandResult;
import io.tebex.sdk.util.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Bukkit platform.
 */
public final class TebexPlugin extends JavaPlugin implements Platform {
    private SDK sdk;
    private ServerPlatformConfig config;
    private boolean setup;
    private PlaceholderManager placeholderManager;
    private Map<Object, Integer> queuedPlayers;
    private YamlDocument configYaml;

    private ServerInformation storeInformation;
    private List<Category> storeCategories;
    private List<ServerEvent> serverEvents;
    public BuyGUI buyGUI;

    /**
     * Starts the Bukkit platform.
     */
    @Override
    public void onEnable() {
        // Bind SDK.
        Tebex.init(this);

        try {
            // Load the platform config file.
            configYaml = initPlatformConfig();
            config = loadServerPlatformConfig(configYaml);
        } catch (IOException e) {
            warning("Failed to load configuration: " + e.getMessage(),
                    "Check that your configuration is valid and in the proper format and reload the plugin. You may delete `Tebex/config.yml` and a new configuration will be generated.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Initialise Managers.
        new CommandManager(this).register();

        // Initialise SDK.
        sdk = new SDK(this, config.getSecretKey());
        placeholderManager = new PlaceholderManager();
        queuedPlayers = Maps.newConcurrentMap();
        storeCategories = new ArrayList<>();
        serverEvents = new ArrayList<>();
        buyGUI = new BuyGUI(this);

        placeholderManager.register(new BukkitNamePlaceholder(placeholderManager));
        placeholderManager.register(new UuidPlaceholder(placeholderManager));


        // Migrate the config from BuycraftX.
        migrateConfig();

        // Initialise the platform.
        init();

        registerEvents(new JoinListener(this));

        getServer().getScheduler().runTaskTimerAsynchronously(this, this::refreshListings, 0, 20 * 60 * 5);

        // every 10 minutes clear the plugin event queue
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            this.getSDK().sendPluginEvents();
        }, 0, 60 * 20 * 10);

        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            List<ServerEvent> runEvents = Lists.newArrayList(serverEvents.subList(0, Math.min(serverEvents.size(), 750)));
            if (runEvents.isEmpty()) return;
            if (!this.isSetup()) return;

            sdk.sendJoinEvents(runEvents)
                    .thenAccept(aVoid -> {
                        serverEvents.removeAll(runEvents);
                        debug("Successfully sent join events.");
                    })
                    .exceptionally(throwable -> {
                        debug("Failed to send join events: " + throwable.getMessage());
                        return null;
                    });
        }, 0, 20 * 60);

        // Register the custom /buy command
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

            if (config.isBuyCommandEnabled()) {
                commandMap.register(getPlatformConfig().getBuyCommandName(), new BuyCommand(getPlatformConfig().getBuyCommandName(), this));
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Failed to get the CommandMap", e);
        }
    }

    public List<Category> getStoreCategories() {
        return storeCategories;
    }

    public ServerInformation getStoreInformation() {
        return storeInformation;
    }

    public List<ServerEvent> getServerEvents() {
        return serverEvents;
    }

    public void migrateConfig() {
        File oldPluginDir = new File("plugins/BuycraftX");
        if (!oldPluginDir.exists()) return;

        File oldConfigFile = new File(oldPluginDir, "config.properties");
        if(!oldConfigFile.exists()) return;

        info("You're running the legacy BuycraftX plugin. Attempting to migrate..");

        try {
            // Load old properties
            Properties properties = new Properties();
            properties.load(Files.newInputStream(oldConfigFile.toPath()));

            String secretKey = properties.getProperty("server-key", null);
            secretKey = !Objects.equals(secretKey, "INVALID") ? secretKey : null;

            if(secretKey != null) {
                // Migrate their existing config.
                configYaml.set("buy-command.name", properties.getProperty("buy-command-name", null));
                configYaml.set("buy-command.enabled", ! Boolean.parseBoolean(properties.getProperty("disable-buy-command", null)));

                configYaml.set("check-for-updates", properties.getOrDefault("check-for-updates", null));
                configYaml.set("verbose", properties.getOrDefault("verbose", false));

                configYaml.set("server.proxy", properties.getOrDefault("is-bungeecord", false));
                configYaml.set("server.secret-key", secretKey);

                // Save new config
                configYaml.save();

                config = loadServerPlatformConfig(configYaml);

                sdk = new SDK(this, config.getSecretKey());

                info("Successfully migrated your config from BuycraftX.");
            }

            // If BuycraftX is installed, delete it.
            boolean legacyPluginEnabled = Bukkit.getPluginManager().isPluginEnabled("BuycraftX");
            boolean deletedLegacyPluginJar = false;

            if(legacyPluginEnabled) {
                try {
                    JavaPlugin plugin = (JavaPlugin) getServer().getPluginManager().getPlugin("BuycraftX");

                    if(plugin != null) {
                        Method getFileMethod = JavaPlugin.class.getDeclaredMethod("getFile");
                        getFileMethod.setAccessible(true);
                        File file = (File) getFileMethod.invoke(plugin);

                        Bukkit.getPluginManager().disablePlugin(plugin);
                        deletedLegacyPluginJar = file.delete();
                    }
                } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                    warning("Failed to disable legacy BuycraftX plugin: " + e.getMessage(), "Please remove it manually from your /plugins folder.");
                }
            }

            boolean deletedLegacyPluginDir = FileUtils.deleteDirectory(oldPluginDir);
            if(!deletedLegacyPluginDir || !deletedLegacyPluginJar) {
                warning("Failed to fully delete the BuycraftX files.", "Please delete them manually in your /plugins folder to avoid conflicts.");
            }
        } catch (IOException e) {
            warning("Failed to migrate BuycraftX configuration: " + e.getMessage(), "Please set your secret key with /tebex secret <key> to enable your store.");
        }
    }

    @Override
    public int getFreeSlots(Object playerId) {
        Player player = getPlayer(playerId);
        if (player == null) return -1;

        ItemStack[] inv = player.getInventory().getContents();

        // Only get the first 36 slots
        inv = Arrays.copyOfRange(inv, 0, 36);

        return (int) Arrays.stream(inv)
                .filter(item -> item == null || item.getType() == Material.AIR)
                .count();
    }

    @Override
    public Map<Object, Integer> getQueuedPlayers() {
        return queuedPlayers;
    }

    public BuyGUI getBuyGUI() {
        return buyGUI;
    }

    public void setBuyGUI(BuyGUI buyGUI) {
        this.buyGUI = buyGUI;
    }

    /**
     * Registers the specified listener with the plugin manager.
     * @param l the listener to register
     */
    public <T extends Listener> void registerEvents(T l) {
        getServer().getPluginManager().registerEvents(l, this);
    }

    @Override
    public PlatformType getType() {
        return PlatformType.BUKKIT;
    }

    @Override
    public SDK getSDK() {
        return sdk;
    }

    @Override
    public File getDirectory() {
        return getDataFolder();
    }

    @Override
    public boolean isSetup() {
        return setup;
    }

    @Override
    public void setSetup(boolean setup) {
        this.setup = setup;
    }

    @Override
    public boolean isOnlineMode() {
        return getServer().getOnlineMode() && ! config.isProxyMode();
    }

    @Override
    public void configure() {
        setup = true;
        performCheck();
        sdk.sendTelemetry();
    }

    @Override
    public void halt() {
        setup = false;
    }

    @Override
    public PlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }

    @Override
    public CommandResult dispatchCommand(String command) {
        if (!isEnabled()) return CommandResult.from(false).withMessage("Store is not enabled.");
        try {
            boolean success = Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
            return CommandResult.from(success);
        } catch (CommandException bukkitCommandException) {
            return CommandResult.from(false).withMessage(bukkitCommandException.getMessage()).withException(bukkitCommandException);
        }
    }

    @Override
    public void executeAsync(Runnable runnable) {
        if (!isEnabled()) return;

        getServer().getScheduler().runTaskAsynchronously(this, runnable);
    }

    @Override
    public void executeAsyncLater(Runnable runnable, long time, TimeUnit unit) {
        if (!isEnabled()) return;

        getServer().getScheduler().runTaskLaterAsynchronously(this, runnable, unit.toMillis(time) / 50);
    }

    @Override
    public void executeBlocking(Runnable runnable) {
        if (!isEnabled()) return;

        getServer().getScheduler().runTask(this, runnable);
    }

    @Override
    public void executeBlockingLater(Runnable runnable, long time, TimeUnit unit) {
        if (!isEnabled()) return;

        getServer().getScheduler().runTaskLater(this, runnable, unit.toMillis(time) / 50);
    }

    public Player getPlayer(Object player) {
        if(player == null) return null;

        if (isOnlineMode() && !isGeyser() && player instanceof UUID) {
            return getServer().getPlayer((UUID) player);
        }

        return getServer().getPlayerExact((String) player);
    }

    @Override
    public boolean isPlayerOnline(Object player) {
        return getPlayer(player) != null;
    }

    @Override
    public void log(Level level, String message) {
        getLogger().log(level, message);
    }

    @Override
    public void setStoreInfo(ServerInformation info) {
        this.storeInformation = info;
    }

    @Override
    public void setStoreCategories(List<Category> categories) {
        this.storeCategories = categories;
    }

    @Override
    public ServerPlatformConfig getPlatformConfig() {
        return config;
    }

    @Override
    public String getVersion() {
        return getDescription().getVersion();
    }

    @Override
    public String getStoreType() {
        return storeInformation == null ? "" : storeInformation.getStore().getGameType();
    }

    @Override
    public PlatformTelemetry getTelemetry() {
        String serverVersion = getServer().getVersion();

        Pattern pattern = Pattern.compile("MC: (\\d+\\.\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(serverVersion);
        if (matcher.find()) {
            serverVersion = matcher.group(1);
        }

        return new PlatformTelemetry(
                getVersion(),
                getServer().getName(),
                serverVersion,
                System.getProperty("java.version"),
                System.getProperty("os.arch"),
                getServer().getOnlineMode()
        );
    }

    @Override
    public String getServerIp() {
        return Bukkit.getIp();
    }
}
