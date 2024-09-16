package io.tebex.sdk.platform;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.sdk.SDK;
import io.tebex.sdk.exception.ServerNotFoundException;
import io.tebex.sdk.exception.ServerNotSetupException;
import io.tebex.sdk.obj.Category;
import io.tebex.sdk.obj.QueuedCommand;
import io.tebex.sdk.obj.QueuedPlayer;
import io.tebex.sdk.placeholder.PlaceholderManager;
import io.tebex.sdk.platform.config.IPlatformConfig;
import io.tebex.sdk.platform.config.ProxyPlatformConfig;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import io.tebex.sdk.request.response.ServerInformation;
import io.tebex.sdk.triage.EnumEventLevel;
import io.tebex.sdk.triage.PluginEvent;
import io.tebex.sdk.util.CommandResult;
import io.tebex.sdk.util.StringUtil;
import io.tebex.sdk.util.UUIDUtil;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static io.tebex.sdk.util.ResourceUtil.getBundledFile;

/**
 * The Platform interface defines the base methods required for interacting with a server platform.
 * Implementations should provide functionality specific to their platform, such as Bukkit or Sponge.
 */
public interface Platform {
    int MAX_COMMANDS_PER_BATCH = 3;

    ArrayList<PluginEvent> PLUGIN_EVENTS = new ArrayList<>();

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
     * Gets the SDK instance associated with this platform.
     *
     * @return The SDK instance.
     */
    SDK getSDK();

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
    boolean isSetup();

    /**
     * Sets whether the platform is set up and ready to use.
     */
    void setSetup(boolean setup);

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
        if (!isSetup()) return false;

        if (getStoreType() == null || getStoreType().isEmpty()) {
            return false;
        }

        return getStoreType().contains("Offline/Geyser");
    }

    /**
     * Configures the platform for use.
     */
    void configure();

    /**
     * Halts the platform and stops any ongoing tasks.
     */
    void halt();

    default void init() {
        if (getPlatformConfig().getSecretKey() != null && !getPlatformConfig().getSecretKey().isEmpty()) {
            getSDK().getServerInformation().thenAccept(serverInformation -> {
                ServerInformation.Server server = serverInformation.getServer();
                ServerInformation.Store store = serverInformation.getStore();

                info(String.format("Connected to %s - %s server.", server.getName(), store.getGameType()));

                setSetup(true);
                configure();
            }).exceptionally(ex -> {
                Throwable cause = ex.getCause();
                setSetup(false);

                if (cause instanceof ServerNotFoundException) {
                    warning("Failed to connect your server.", "Please double-check your server key or run the setup command again.");
                    this.halt();
                } else {
                    warning("Failed to retrieve server information. " + cause.getMessage(), "Please double check your server key or run the seutp command again.", ex);
                }

                return null;
            });
        } else {
            info("Welcome to Tebex! It seems like this is a new setup.");
            info("To get started, please use the 'tebex secret <key>' command in the console.");
        }
    }

    PlaceholderManager getPlaceholderManager();

    Map<Object, Integer> getQueuedPlayers();

    /**
     * Dispatches a command to the server.
     *
     * @param command The command to dispatch.
     */
    CommandResult dispatchCommand(String command);

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
        if(! isSetup()) return;

        debug("Checking for due players...");
        getQueuedPlayers().clear();

        getSDK().getDuePlayers().whenComplete((duePlayersResponse, ex) -> {
            if (runAfter) {
                int nextCheck = duePlayersResponse == null ? 60 : duePlayersResponse.getNextCheck();
                executeAsyncLater(this::performCheck, nextCheck, TimeUnit.SECONDS);
            }
            if (ex != null) {
                warning("Failed to get due players" + ex.getMessage(), "We will try again at the next due player check.", ex);
                return;
            }

            List<QueuedPlayer> playerList = duePlayersResponse.getPlayers();

            if(! playerList.isEmpty()) {
                debug("Found " + playerList.size() + " " + StringUtil.pluralise(playerList.size(), "player", "players") + " with pending commands.");
                playerList.forEach(this::handleOnlineCommands);
            }

            if(! duePlayersResponse.canExecuteOffline()) return;
            handleOfflineCommands();
        });
    }

//    default void sendTriageEvent(Throwable exception) {
//        StringWriter traceWriter = new StringWriter();
//        exception.printStackTrace(new PrintWriter(traceWriter));
//
//        HashMap<String, String> metadata = new HashMap<>();
//        metadata.put("trace", traceWriter.toString());
//        TriageEvent event = TriageEvent.fromPlatform(this)
//                .withErrorMessage(exception.getMessage())
//                .withMetadata(metadata);
//
//        event.send();
//    }

    default void handleOnlineCommands(QueuedPlayer player) {
        if(! isSetup()) return;

        debug("Processing online commands for player '" + player.getName() + "'...");
        Object playerId = getPlayerId(player.getName(), UUIDUtil.mojangIdToJavaId(player.getUuid()));
        if(!isPlayerOnline(playerId)) {
            debug("Player " + player.getName() + " has online commands but is not connected. Skipping.");
            getQueuedPlayers().put(playerId, player.getId()); // will cause commands to be processed when player connects
            return;
        }

        getSDK().getOnlineCommands(player).thenAccept(onlineCommands -> {
            if(onlineCommands.isEmpty()) {
                debug("No commands found for " + player.getName() + ".");
                return;
            }

            debug("Found " + onlineCommands.size() + " online " + StringUtil.pluralise(onlineCommands.size(), "command") + ".");
            processOnlineCommands(player.getName(), playerId, onlineCommands);
        }).exceptionally(ex -> {
            warning("Failed to get online commands for " + player.getName() + ". " + ex.getMessage(), "We will try again at the next due player check.", ex);
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
        if(! isSetup()) return;

        List<Integer> completedCommands = new ArrayList<>();
        boolean hasInventorySpace = true;
        for (QueuedCommand command : commands) {
            int freeSlots = getFreeSlots(playerId);
            if(freeSlots < command.getRequiredSlots()) {
                info(String.format("Skipping command '%s' for player '%s' due to no inventory space. Free slots: %d. Slots required: %d", command.getParsedCommand(), playerName, freeSlots, command.getRequiredSlots()));
                hasInventorySpace = false;
                continue;
            }

            executeBlocking(() -> {
                info(String.format("Dispatching command '%s' for player '%s'", command.getParsedCommand(), playerName));
                CommandResult commandResult = dispatchCommand(command.getParsedCommand());

                // report whether the command succeeded or failed
                if (!commandResult.getIsSuccess()) {
                    String extraInfo = "";
                    Throwable commandException = commandResult.getException();
                    if (commandResult.getMessage() != null && !commandResult.getMessage().isEmpty()) {
                        extraInfo = commandResult.getMessage();
                    }
                    if (commandException != null) {
                        extraInfo = commandResult.getException().getMessage();
                    }

                    String solution = "Check that the command syntax is correct.";
                    if (command.getPayment() != 0) {
                        solution += " Re-run this command at https://creator.tebex.io/payments/" + command.getPayment();
                    }
                    warning(String.format("Command `%s` failed to execute: %s", command.getParsedCommand(), extraInfo), solution);
                }
            });
            // At present all queued commands are reported as successful once delivery criteria are met, regardless if dispatching
            // the command worked without errors. We *could* refactor this to only mark commands completed if they were successful, but
            // platform-specific support for actually reporting if a command was successful and why or why not is dubious at best.
            // This could also lead to a situation where massive stores have a continuously growing queue of bad commands, which would have to be manually invalidated and then re-sent.
            // By marking all queued commands completed meeting in-game delivery criteria, this allows the store to visit a previously invalid command and re-run it directly from their store panel.
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
        if(! isSetup()) return;

        getSDK().getOfflineCommands().thenAccept(offlineData -> {
            if(offlineData.getCommands().isEmpty()) {
                return;
            }

            List<Integer> completedCommands = new ArrayList<>();
            for (QueuedCommand command : offlineData.getCommands()) {
                executeBlockingLater(() -> {
                    info(String.format("Dispatching offline command '%s' for player '%s'.", command.getParsedCommand(), command.getPlayer().getName()));
                    CommandResult offlineCommandResult = dispatchCommand(command.getParsedCommand());

                    // report whether the offline command succeeded or failed
                    if (!offlineCommandResult.getIsSuccess()) {
                        String extraInfo = "";
                        Throwable commandException = offlineCommandResult.getException();
                        if (!offlineCommandResult.getMessage().isEmpty()) {
                            extraInfo = offlineCommandResult.getMessage();
                        }
                        if (commandException != null) {
                            extraInfo = offlineCommandResult.getException().getMessage();
                        }

                        String solution = "Check that the command syntax is correct.";
                        if (command.getPayment() != 0) {
                            solution += " Re-run this command at https://creator.tebex.io/payments/" + command.getPayment();
                        }
                        warning(String.format("Command `%s` failed to execute: %s", command.getParsedCommand(), extraInfo), solution);
                    }
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
            warning("Failed to retrieve offline commands - some commands may not have been processed. " + ex.getMessage(), "We will try again at the next due player check.", ex);
            return null;
        });
    }

    default void deleteCompletedCommands(List<Integer> completedCommands) {
        getSDK().deleteCommands(completedCommands).thenRun(completedCommands::clear).exceptionally(ex -> {
            error("Failed to delete commands: " + ex.getMessage(), ex);
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
     * Logs a warning message to the console. A "warning" is due to a problem that either the user can solve, or a
     * problem that can resolve itself later. All warnings must have solutions provided.
     *
     * ex.)
     * @param message The message to log.
     * @param solution User-friendly description of how to resolve the problem.
     */
    default void warning(String message, String solution) {
        log(Level.WARNING, message);
        log(Level.WARNING, "- " + solution);

        if (getPlatformConfig().isAutoReportEnabled()) {
            PLUGIN_EVENTS.add(new PluginEvent(this, EnumEventLevel.WARNING, message));
        }
    }

    default void warning(String message, String solution, Throwable t) {
        log(Level.WARNING, message);
        log(Level.WARNING, "- " + solution);

        if (getPlatformConfig().isAutoReportEnabled()) {
            PLUGIN_EVENTS.add(new PluginEvent(this, EnumEventLevel.WARNING, message).withTrace(t));
        }
    }

    default void error(String message) {
        log(Level.SEVERE, message);
        if (getPlatformConfig().isAutoReportEnabled()) {
            PLUGIN_EVENTS.add(new PluginEvent(this, EnumEventLevel.ERROR, message));
        }
    }

    default void error(String message, Throwable t) {
        log(Level.SEVERE, message);
        if (getPlatformConfig().isAutoReportEnabled()) {
            PLUGIN_EVENTS.add(new PluginEvent(this, EnumEventLevel.ERROR, message).withTrace(t));
        } else { // trace is printed when added above, but would be skipped if auto report was disabled. print it here
            t.printStackTrace();
        }
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

        config.setSecretKey(configFile.getString("server.secret-key"));
        config.setBuyCommandName(configFile.getString("buy-command.name", "buy"));
        config.setBuyCommandEnabled(configFile.getBoolean("buy-command.enabled", true));

        config.setCheckForUpdates(configFile.getBoolean("check-for-updates", true));
        config.setVerbose(configFile.getBoolean("verbose", false));

        config.setProxyMode(configFile.getBoolean("server.proxy", false));
        config.setAutoReportEnabled(configFile.getBoolean("auto-report-enabled", true));

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
        getSDK().getServerInformation().thenAccept(this::setStoreInfo);
        getSDK().getListing().thenAccept(this::setStoreCategories);
    }

    void setStoreInfo(ServerInformation info);

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
}
