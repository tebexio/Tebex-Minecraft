package io.tebex.plugin;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.analytics.AnalyticsService;
import io.tebex.plugin.analytics.hook.FloodgateHook;
import io.tebex.plugin.store.StoreService;
import io.tebex.plugin.store.listener.JoinListener;
import io.tebex.sdk.store.SDK;
import io.tebex.sdk.Tebex;
import io.tebex.sdk.platform.Platform;
import io.tebex.sdk.platform.PlatformTelemetry;
import io.tebex.sdk.platform.PlatformType;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import io.tebex.sdk.store.obj.Category;
import io.tebex.sdk.store.obj.ServerEvent;
import io.tebex.sdk.store.placeholder.PlaceholderManager;
import io.tebex.sdk.store.response.ServerInformation;
import io.tebex.sdk.util.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import space.arim.morepaperlib.MorePaperLib;
import space.arim.morepaperlib.scheduling.AsynchronousScheduler;
import space.arim.morepaperlib.scheduling.RegionalScheduler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Bukkit/Spigot plugin for Tebex.
 */
public final class TebexPlugin extends JavaPlugin implements Platform {
    private ServerPlatformConfig config;
    private YamlDocument configYaml;
    private StoreService storeService;
    private AnalyticsService analyticsService;
    private FloodgateHook floodgateHook;
    private MorePaperLib morePaperLib;

    /**
     * Starts the Bukkit plugin.
     */
    @Override
    public void onEnable() {
        // Bind SDK.
        Tebex.init(this);

        morePaperLib = new MorePaperLib(this);

        try {
            // Load the platform config file.
            configYaml = initPlatformConfig();
            config = loadServerPlatformConfig(configYaml);
        } catch (IOException e) {
            log(Level.WARNING, "Failed to load config: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        config.setFloodgateHook(getServer().getPluginManager().isPluginEnabled("floodgate"));

        if(config.isFloodgateHookEnabled()) {
            floodgateHook = new FloodgateHook();
        }

        boolean storeSetup = getPlatformConfig().getStoreSecretKey() != null && ! getPlatformConfig().getStoreSecretKey().isEmpty();
        boolean analyticsSetup = getPlatformConfig().getAnalyticsSecretKey() != null && ! getPlatformConfig().getAnalyticsSecretKey().isEmpty();

        String messagePart = !storeSetup && !analyticsSetup ? "Store and Analytics" : !storeSetup ? "Store" : !analyticsSetup ? "Analytics" : "";

        info("Thanks for installing Tebex v" + getDescription().getVersion() + " for Spigot/Paper.");

        if(! storeSetup || ! analyticsSetup) {
            warning("It seems that you're using a fresh install, or haven't configured your " + messagePart + " secret keys yet!");
            warning(" ");

            if(! storeSetup) {
                warning("To setup your Tebex Store, run 'tebex secret <key>' in the console.");
            }
            if(! analyticsSetup) {
                warning("To setup your Tebex Analytics, run 'analytics secret <key>' in the console.");
            }

            warning(" ");
            warning("We recommend running these commands from the console to avoid accidentally sharing your secret keys in chat.");
        }

        // Migrate the config from BuycraftX.
        migrateConfig();

        // TODO: Migrate Analyse

        // Initialise Managers.
        storeService = new StoreService(this);
        storeService.init();

        if(storeSetup) {
            storeService.connect();
        }

        analyticsService = new AnalyticsService(this);
        analyticsService.init();

        if(analyticsSetup) {
            analyticsService.connect();
        }

        registerEvents(new JoinListener(this));

    }

    public List<Category> getStoreCategories() {
        return storeService.getStoreCategories();
    }

    public ServerInformation getStoreInformation() {
        return storeService.getStoreInformation();
    }

    public List<ServerEvent> getServerEvents() {
        return storeService.getServerEvents();
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

                storeService = new StoreService(this);
                storeService.init();
                storeService.connect();

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
                    // Failed to get the plugin file via reflection.
                    warning("Failed to delete the old BuycraftX files: " + e.getMessage());
                }
            }

            boolean deletedLegacyPluginDir = FileUtils.deleteDirectory(oldPluginDir);
            if(! deletedLegacyPluginDir || !deletedLegacyPluginJar) {
                warning("Failed to delete the old BuycraftX files. Please delete them manually in your /plugins folder to avoid conflicts.");
            }
        } catch (IOException e) {
            warning("Failed to migrate config: " + e.getMessage());
            e.printStackTrace();
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
        return storeService.getQueuedPlayers();
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
    public SDK getStoreSDK() {
        return storeService.getSdk();
    }

    @Override
    public io.tebex.sdk.analytics.SDK getAnalyticsSDK() {
        return analyticsService.getSdk();
    }

    @Override
    public File getDirectory() {
        return getDataFolder();
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
        return getServer().getOnlineMode() && ! config.isProxyMode();
    }

    @Override
    public void halt() {
        storeService.setSetup(false);
    }

    @Override
    public PlaceholderManager getPlaceholderManager() {
        return storeService.getPlaceholderManager();
    }

    public MorePaperLib getPaperLib() {
        return morePaperLib;
    }

    @Override
    public void dispatchCommand(String command) {
        if (!isEnabled()) return;

        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    @Override
    public void executeAsync(Runnable runnable) {
        if (!isEnabled()) return;

        getAsyncScheduler().run(runnable);
    }

    @Override
    public void executeAsyncLater(Runnable runnable, long time, TimeUnit unit) {
        if (!isEnabled()) return;

        getAsyncScheduler().runDelayed(runnable, Duration.ofMillis(unit.toMillis(time)));
    }

    @Override
    public void executeBlocking(Runnable runnable) {
        if (!isEnabled()) return;

        getScheduler().run(runnable);
    }

    @Override
    public void executeBlockingLater(Runnable runnable, long time, TimeUnit unit) {
        if (!isEnabled()) return;

        getScheduler().runDelayed(runnable, unit.toMillis(time));
    }

    public Player getPlayer(Object player) {
        if(player == null) return null;

        if (isOnlineMode()) {
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
    public void setStoreInformation(ServerInformation info) {
        storeService.setStoreInformation(info);
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
    public String getVersion() {
        return getDescription().getVersion();
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

    @Override
    public boolean isPlayerExcluded(UUID uniqueId) {
        return getPlatformConfig().isPlayerExcluded(uniqueId);
    }

    public StoreService getStoreManager() {
        return storeService;
    }

    public AnalyticsService getAnalyticsManager() {
        return analyticsService;
    }

    public FloodgateHook getFloodgateHook() {
        return floodgateHook;
    }

    public RegionalScheduler getScheduler() {
        return morePaperLib.scheduling().globalRegionalScheduler();
    }

    public AsynchronousScheduler getAsyncScheduler() {
        return morePaperLib.scheduling().asyncScheduler();
    }

    public void sendMessage(CommandSender sender, String message) {
        String str = ChatColor.translateAlternateColorCodes('&', "&b[Tebex] &7" + message);

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.stripColor(str));
            return;
        }

        sender.sendMessage(str);
    }
}
