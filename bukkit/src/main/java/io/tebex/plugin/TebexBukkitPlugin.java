package io.tebex.plugin;

import com.google.common.collect.Lists;
import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.command.BuyCommand;
import io.tebex.plugin.command.TebexCommandExecutor;
import io.tebex.plugin.event.InventoryClickListener;
import io.tebex.plugin.event.join.HuskSyncPlayerJoinListener;
import io.tebex.plugin.event.join.PlayerJoinListener;
import io.tebex.plugin.placeholder.BukkitNamePlaceholder;
import io.tebex.sdk.Tebex;
import io.tebex.sdk.obj.ServerEvent;
import io.tebex.sdk.placeholder.PlaceholderManager;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

/**
 * The Bukkit platform.
 */
public final class TebexBukkitPlugin extends JavaPlugin {
    private BukkitPluginPlatform platform;

    public BukkitPluginPlatform getPlatform() {
        return platform;
    }

    /**
     * Starts the Bukkit platform.
     */
    @Override
    public void onEnable() {
        platform = new BukkitPluginPlatform(this);
        Tebex.init(platform);

        migrateConfig(); // Migrate old config from BuycraftX

        platform.loadPlatformConfig(); // loads the configuration file for the platform

        platform.initStore(); // uses loaded key to set current store and cache the available packages

        // Bukkit specific registration
        TebexCommandExecutor tebexCommands = new TebexCommandExecutor(platform);
        PluginCommand pluginCommand = platform.getPlugin().getCommand("tebex");
        if (pluginCommand == null) {
            throw new RuntimeException("Tebex command not found.");
        }
        pluginCommand.setExecutor(tebexCommands);
        pluginCommand.setTabCompleter(tebexCommands);

        registerBuyCommandIfEnabled();

        if (getServer().getPluginManager().isPluginEnabled("HuskSync")) {
            registerEvents(new HuskSyncPlayerJoinListener(platform));
        } else {
            registerEvents(new PlayerJoinListener(platform));
        }

        registerEvents(new InventoryClickListener());

        PlaceholderManager placeholderManager = platform.getPlaceholderManager();
        placeholderManager.register(new BukkitNamePlaceholder(placeholderManager));

        // Refresh store listings every 5 minutes
        getServer().getScheduler().runTaskTimerAsynchronously(this, platform::refreshListings, 0, 20 * 60 * 5);

        // Every 10 minutes clear the plugin event queue
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> platform.getSDK().sendPluginEvents(), 0, 60 * 20 * 10);

        // clear server events every minute
        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            List<ServerEvent> allServerEvents = platform.getJoinEvents();
            List<ServerEvent> runEvents;
            synchronized (allServerEvents) {
                runEvents = Lists.newArrayList(allServerEvents.subList(0, Math.min(allServerEvents.size(), 750)));
            }
            if (runEvents.isEmpty()) return;
            if (!platform.isSetup()) return;

            platform.getSDK().sendJoinEvents(runEvents)
                    .thenAccept(aVoid -> {
                        platform.clearSelectedPluginEvents(runEvents);
                        platform.debug("Successfully sent join events.");
                    })
                    .exceptionally(throwable -> {
                        platform.debug("Failed to send join events: " + throwable.getMessage());
                        return null;
                    });
        }, 0, 20 * 60);

        // start the initial check, which is rescheduled according to the next_check from remote
        //platform.checkCommandQueue(true);
    }

    /**
     * Registers the specified listener with the plugin manager.
     * @param l the listener to register
     */
    public <T extends Listener> void registerEvents(T l) {
        getServer().getPluginManager().registerEvents(l, this);
    }

    public void migrateConfig() {
        File oldPluginDir = new File("plugins/BuycraftX");
        if (!oldPluginDir.exists()) return;

        File oldConfigFile = new File(oldPluginDir, "config.properties");
        if(!oldConfigFile.exists()) return;

        platform.info("Detected legacy BuycraftX configuration. Attempting to migrate...");

        try {
            // Load old properties
            Properties properties = new Properties();
            properties.load(Files.newInputStream(oldConfigFile.toPath()));

            String secretKey = properties.getProperty("server-key", null);
            secretKey = !Objects.equals(secretKey, "INVALID") ? secretKey : null;

            if(secretKey != null) {
                YamlDocument configYaml = platform.initPlatformConfig();
                // Migrate their existing config.
                configYaml.set("buy-command.name", properties.getProperty("buy-command-name", null));
                configYaml.set("buy-command.enabled", ! Boolean.parseBoolean(properties.getProperty("disable-buy-command", null)));

                configYaml.set("check-for-updates", properties.getOrDefault("check-for-updates", null));
                configYaml.set("verbose", properties.getOrDefault("verbose", false));

                configYaml.set("server.proxy", properties.getOrDefault("is-bungeecord", false));
                configYaml.set("server.secret-key", secretKey);

                // Save new config
                configYaml.save();

                platform.setPlatformConfigYaml(configYaml);
                platform.setConfig(platform.loadServerPlatformConfig(configYaml));
                platform.setSecretKey(secretKey);

                platform.info("Successfully migrated your config from BuycraftX.");
            }

            // If BuycraftX is installed, delete the plugin JAR.
            boolean legacyPluginEnabled = Bukkit.getPluginManager().isPluginEnabled("BuycraftX");
            if(legacyPluginEnabled) {
                try {
                    JavaPlugin plugin = (JavaPlugin) getServer().getPluginManager().getPlugin("BuycraftX");

                    if(plugin != null) {
                        Method getFileMethod = JavaPlugin.class.getDeclaredMethod("getFile");
                        getFileMethod.setAccessible(true);
                        File file = (File) getFileMethod.invoke(plugin);

                        Bukkit.getPluginManager().disablePlugin(plugin);
                        boolean deletedLegacyPluginJar = file.delete();
                        if(!deletedLegacyPluginJar) {
                            platform.info("Failed to fully delete the legacy BuycraftX plugin.");
                            platform.info("Please delete it manually in your /plugins folder to avoid conflicts.");
                        }
                    }
                } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                    platform.warning("Failed to disable legacy BuycraftX plugin: " + e.getMessage(), "Please remove it manually from your /plugins folder.");
                }
            }
        } catch (IOException e) {
            platform.warning("Failed to migrate BuycraftX configuration: " + e.getMessage(), "Please set your secret key with /tebex secret <key> to enable your store.");
        }
    }

    public void registerBuyCommandIfEnabled() {
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

            ServerPlatformConfig config = (ServerPlatformConfig) platform.getPlatformConfig();
            if (config.isBuyCommandEnabled()) {
                commandMap.register(config.getBuyCommandName(), new BuyCommand(config.getBuyCommandName(), platform));
            }
        } catch (Throwable e) {
            platform.error("Failed to register buy command: " + e.getMessage(), e);
        }
    }
}
