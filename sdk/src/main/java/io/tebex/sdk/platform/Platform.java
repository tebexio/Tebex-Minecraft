package io.tebex.sdk.platform;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.sdk.platform.config.IPlatformConfig;
import io.tebex.sdk.platform.config.ProxyPlatformConfig;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import io.tebex.sdk.platform.service.PlayerCountService;
import io.tebex.sdk.store.SDK;
import io.tebex.sdk.store.obj.Category;
import io.tebex.sdk.store.obj.QueuedCommand;
import io.tebex.sdk.store.obj.QueuedPlayer;
import io.tebex.sdk.store.placeholder.PlaceholderManager;
import io.tebex.sdk.store.response.ServerInformation;
import io.tebex.sdk.store.triage.TriageEvent;
import io.tebex.sdk.util.StringUtil;
import io.tebex.sdk.util.UUIDUtil;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static io.tebex.sdk.util.ResourceUtil.getBundledFile;

/**
 * The Platform interface defines the base methods required for interacting with a server platform.
 * Implementations should provide functionality specific to their platform, such as Bukkit or Sponge.
 */
public interface Platform {
    int MAX_COMMANDS_PER_BATCH = 3;

    /**
     * Gets the platform type.
     *
     * @return The PlatformType enum value representing the server platform.
     */
    PlatformType getType();

    /**
     * Gets the store's type from store info. This info should be cached and not fetched on each request
     *
     * @return Store info string ex. "Minecraft (Offline/Geyser)"
     */
    String getStoreType();

    /**
     * Gets the Store SDK instance associated with this platform.
     *
     * @return The SDK instance.
     */
    SDK getStoreSDK();

    /**
     * Gets the Analytics SDK instance associated with this platform.
     *
     * @return The SDK instance.
     */
    default io.tebex.sdk.analytics.SDK getAnalyticsSDK() {
        throw new UnsupportedOperationException("getAnalyticsSDK is not implemented");
    }

    /**
     * Gets the directory where the plugin is running from.
     *
     * @return The directory.
     */
    File getDirectory();

    /**
     * Checks if the platform is set up and ready to use.
     *
     * @return True if the platform is set up, false otherwise.
     */
    boolean isStoreSetup();

    default boolean isAnalyticsSetup() {
        return false;
    }

    /**
     * Checks if the platform is in online mode.
     *
     * @return Whether the server is in online mode.
     */
    boolean isOnlineMode();

    /**
     * Checks if the configured store is Geyser/Offline
     *
     * @return Whether the store is a Offline/Geyser type webstore
     */
    default boolean isGeyser() {
        if (!isStoreSetup()) return false;

        if (getStoreType() == null || getStoreType().isEmpty()) {
            return false;
        }

        return getStoreType().contains("Offline/Geyser");
    }

    /**
     * Configures the platform for use.
     */
    default void configure() {

    }

    /**
     * Halts the platform and stops any ongoing tasks.
     */
    void halt();

    PlaceholderManager getPlaceholderManager();

    Map<Object, Integer> getQueuedPlayers();

    /**
     * Dispatches a command to the server.
     * @param command The command to dispatch.
     */
    void dispatchCommand(String command);

    void executeAsync(Runnable runnable);
    void executeAsyncLater(Runnable runnable, long time, TimeUnit unit);
    void executeBlocking(Runnable runnable);
    void executeBlockingLater(Runnable runnable, long time, TimeUnit unit);
    boolean isPlayerOnline(Object player);
    int getFreeSlots(Object player);

    default void performCheck() {
        performCheck(true);
    }

    default void performCheck(boolean runAfter) {
        if(! isStoreSetup()) return;

        debug("Checking for due players..");
        getQueuedPlayers().clear();

        getStoreSDK().getDuePlayers().thenAccept(duePlayersResponse -> {
            if(runAfter) {
                executeAsyncLater(this::performCheck, duePlayersResponse.getNextCheck(), TimeUnit.SECONDS);
            }

            List<QueuedPlayer> playerList = duePlayersResponse.getPlayers();

            if(! playerList.isEmpty()) {
                debug("Found " + playerList.size() + " " + StringUtil.pluralise(playerList.size(), "player", "players") + " with pending commands.");
                playerList.forEach(this::handleOnlineCommands);
            }

            if(! duePlayersResponse.canExecuteOffline()) return;
            handleOfflineCommands();
        }).exceptionally(ex -> {
            warning("Failed to perform check: " + ex.getMessage());
            ex.printStackTrace();
            sendTriageEvent(ex);
            return null;
        });
    }

    default void sendTriageEvent(String errorMessage) {
        TriageEvent.fromPlatform(this).withErrorMessage(errorMessage).send();
    }

    default void sendTriageEvent(Throwable exception) {
        StringWriter traceWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(traceWriter));

        TriageEvent event = TriageEvent.fromPlatform(this)
                .withErrorMessage(exception.getMessage())
                .withTrace(traceWriter.toString());

        event.send();
    }

    default void handleOnlineCommands(QueuedPlayer player) {
        if(! isStoreSetup()) return;

        debug("Processing online commands for player '" + player.getName() + "'...");
        Object playerId = getPlayerId(player.getName(), UUIDUtil.mojangIdToJavaId(player.getUuid()));

        if(!isPlayerOnline(playerId)) {
            debug("Player " + player.getName() + " has online commands but is not connected. Skipping.");
            getQueuedPlayers().put(playerId, player.getId()); // will cause commands to be processed when player connects
            return;
        }

        getStoreSDK().getOnlineCommands(player).thenAccept(onlineCommands -> {
            if(onlineCommands.isEmpty()) {
                debug("No commands found for " + player.getName() + ".");
                return;
            }

            debug("Found " + onlineCommands.size() + " online " + StringUtil.pluralise(onlineCommands.size(), "command") + ".");
            processOnlineCommands(player.getName(), playerId, onlineCommands);
        }).exceptionally(ex -> {
            warning("Failed to get online commands for " + player.getName() + ": " + ex.getMessage());
            ex.printStackTrace();
            sendTriageEvent(ex);
            return null;
        });
    }

    /**
     * Selects the appropriate player ID for a player based on platform configuration.
     * @param name The name of the player.
     * @param uuid The UUID of the player.
     * @return The player ID to use.
     */
    @NotNull default Object getPlayerId(String name, UUID uuid) {
        // online mode uses uuids while offline mode uses usernames. default to the name if we ever fail to have a uuid
        Object identifier = isOnlineMode() ? uuid : name;
        if (identifier == null) {
            identifier = (name == null) ? "" : name;
        }

        return identifier;
    }

    /**
     * Processes the online commands for a player.
     *
     * @param playerName The name of the player.
     * @param playerId The Unique Identifier of the player.
     * @param commands The commands to process.
     */
    default void processOnlineCommands(String playerName, Object playerId, List<QueuedCommand> commands) {
        if(! isStoreSetup()) return;

        List<Integer> completedCommands = new ArrayList<>();
        boolean hasInventorySpace = true;
        for (QueuedCommand command : commands) {
            if(getFreeSlots(playerId) < command.getRequiredSlots()) {
                debug(String.format("Skipping command '%s' for player '%s' due to no inventory space.", command.getParsedCommand(), playerName));
                hasInventorySpace = false;
                continue;
            }

            executeBlocking(() -> {
                info(String.format("Dispatching command '%s' for player '%s'.", command.getParsedCommand(), playerName));
                dispatchCommand(command.getParsedCommand());
            });
            completedCommands.add(command.getId());

            if(completedCommands.size() % MAX_COMMANDS_PER_BATCH == 0) {
                deleteCompletedCommands(completedCommands);
                completedCommands.clear();
            }
        }

        if (!completedCommands.isEmpty()) {
            deleteCompletedCommands(completedCommands);
            completedCommands.clear();
        }

        if(! hasInventorySpace) return;
        getQueuedPlayers().remove(playerId);
    }

    default void handleOfflineCommands() {
        if(! isStoreSetup()) return;

        getStoreSDK().getOfflineCommands().thenAccept(offlineData -> {
            if(offlineData.getCommands().isEmpty()) {
                return;
            }

            List<Integer> completedCommands = new ArrayList<>();
            for (QueuedCommand command : offlineData.getCommands()) {
                executeBlockingLater(() -> {
                    info(String.format("Dispatching offline command '%s' for player '%s'.", command.getParsedCommand(), command.getPlayer().getName()));
                    dispatchCommand(command.getParsedCommand());
                }, command.getDelay(), TimeUnit.SECONDS);
                completedCommands.add(command.getId());

                if(completedCommands.size() % MAX_COMMANDS_PER_BATCH == 0) {
                    deleteCompletedCommands(completedCommands);
                    completedCommands.clear();
                }
            }

            if (! completedCommands.isEmpty()) {
                deleteCompletedCommands(completedCommands);
                completedCommands.clear();
            }
        }).exceptionally(ex -> {
            warning("Failed to get offline commands: " + ex.getMessage());
            ex.printStackTrace();
            sendTriageEvent(ex);
            return null;
        });
    }

    default void deleteCompletedCommands(List<Integer> completedCommands) {
        getStoreSDK().deleteCommands(completedCommands).thenRun(completedCommands::clear).exceptionally(ex -> {
            warning("Failed to delete commands: " + ex.getMessage());
            ex.printStackTrace();
            sendTriageEvent(ex);
            return null;
        });

    }

    /**
     * Gets the version of the platform implementation.
     *
     * @return The version string.
     */
    String getVersion();

    /**
     * Converts the version string into a version number.
     *
     * @return The version number.
     */
    default int getVersionNumber() {
        return Integer.parseInt(getVersion().replace(".", ""));
    }

    /**
     * Logs a message to the console with the specified level.
     *
     * @param level   The level of the message.
     * @param message The message to log.
     */
    void log(Level level, String message);

    /**
     * Logs an informational message to the console.
     *
     * @param message The message to log.
     */
    default void info(String message) {
        log(Level.INFO, message);
    }

    /**
     * Logs a warning message to the console.
     *
     * @param message The message to log.
     */
    default void warning(String message) {
        log(Level.WARNING, message);
    }

    /**
     * Logs a debug message to the console if debugging is enabled in the platform configuration.
     *
     * @param message The message to log.
     */
    default void debug(String message) {
        if (! getPlatformConfig().isVerbose()) return;
        info("[DEBUG] " + message);
    }

    // Create and update the file
    default YamlDocument initPlatformConfig() throws IOException {
        return YamlDocument.create(getBundledFile(this, getDirectory(), "config.yml"));
    }

    /**
     * Loads the server platform configuration from the file.
     *
     * @param configFile The configuration file.
     * @return The PlatformConfig instance representing the loaded configuration.
     */
    default ServerPlatformConfig loadServerPlatformConfig(YamlDocument configFile) {
        ServerPlatformConfig config = new ServerPlatformConfig(configFile.getInt("config-version", 1));
        config.setYamlDocument(configFile);

        if(config.getConfigVersion() < 2) {
            return config;
        }

        /* Tebex Store */
        config.setStoreSecretKey(configFile.getString("server.secret-key"));
        config.setBuyCommandName(configFile.getString("buy-command.name", "buy"));
        config.setBuyCommandEnabled(configFile.getBoolean("buy-command.enabled", true));

        config.setCheckForUpdates(configFile.getBoolean("check-for-updates", true));
        config.setVerbose(configFile.getBoolean("verbose", false));

        config.setProxyMode(configFile.getBoolean("server.proxy", false));
        config.setAutoReportEnabled(configFile.getBoolean("auto-report-enabled", true));

        /* Tebex Analytics */
        config.setExcludedPlayers(configFile.getStringList("analytics.excluded-players").stream().map(UUID::fromString).collect(Collectors.toList()));
        config.setUseServerFirstJoinedAt(configFile.getBoolean("analytics.use-server-playtime", false));
        config.setBedrockPrefix(configFile.getString("analytics.bedrock-prefix"));
        config.setAnalyticsSecretKey(configFile.getString("analytics.secret-key"));
        config.setDeveloperMode(configFile.getBoolean("analytics.developer-mode", false));
        config.setMultiInstance(configFile.getBoolean("analytics.multi-instance", false));

        return config;
    }

    /**
     * Loads the proxy platform configuration from the file.
     *
     * @param configFile The configuration file.
     * @return The PlatformConfig instance representing the loaded configuration.
     */
    default ProxyPlatformConfig loadProxyPlatformConfig(YamlDocument configFile) {
        ProxyPlatformConfig config = new ProxyPlatformConfig(configFile.getInt("config-version", 1));
        config.setYamlDocument(configFile);

        if(config.getConfigVersion() < 2) {
            return config;
        }

        config.setSecretKey(configFile.getString("server.secret-key"));
        config.setVerbose(configFile.getBoolean("verbose", false));

        return config;
    }

    default void refreshListings() {
        getStoreSDK().getServerInformation()
                .thenAccept(this::setStoreInformation)
                .exceptionally(ex -> {
                    warning("Failed to get server information: " + ex.getMessage());
                    sendTriageEvent(ex);
                    return null;
                });
        getStoreSDK().getListing()
                .thenAccept(this::setStoreCategories).exceptionally(ex -> {
            warning("Failed to get store categories: " + ex.getMessage());
            sendTriageEvent(ex);
            return null;
        });
    }

    void setStoreInformation(ServerInformation info);

    void setStoreCategories(List<Category> categories);

    /**
     * Gets the current platform configuration.
     *
     * @return The PlatformConfig instance representing the current configuration.
     */
    IPlatformConfig getPlatformConfig();

    /**
     * Gets the platform telemetry instance.
     *
     * @return The PlatformTelemetry instance.
     */
    PlatformTelemetry getTelemetry();

    /**
     * Gets the current server's IP address.
     *
     * @return IP address of the server as a string
     */
    String getServerIp();

    default PlayerCountService getPlayerCountService() {
        throw new UnsupportedOperationException("getPlayerCountService() is not implemented");
    }

    default boolean isPlayerExcluded(UUID uniqueId) {
        throw new UnsupportedOperationException("isPlayerExcluded(UUID) is not implemented");
    }

    default void printSetupMessage(boolean storeSetup, boolean analyticsSetup) {
        String messagePart = !storeSetup && !analyticsSetup ? "Webstore and Analytics" : !storeSetup ? "Webstore" : !analyticsSetup ? "Analytics" : "";

        info("Thanks for installing Tebex v" + getVersion() + " for " + getType().getName() + ".");

        if (!storeSetup || !analyticsSetup) {
            warning("It seems that you're using a fresh install, or haven't configured your " + messagePart + " secret keys yet!");
            info(" ");

            if (!storeSetup) {
                info("Run 'tebex secret <key>' in the console to setup your Tebex Webstore.");
            }
            if (!analyticsSetup) {
                info("Run 'analytics secret <key>' in the console to setup your Tebex Analytics.");
            }

            info(" ");
            warning("We recommend running these commands from the console, to avoid accidentally sharing your secret keys in chat.");
        }
    }
}
