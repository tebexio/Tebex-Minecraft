package io.tebex.sdk.platform;

import com.google.common.collect.Maps;
import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.sdk.SDK;
import io.tebex.sdk.exception.ServerNotFoundException;
import io.tebex.sdk.obj.*;
import io.tebex.sdk.placeholder.PlaceholderManager;
import io.tebex.sdk.placeholder.defaults.UuidPlaceholder;
import io.tebex.sdk.platform.config.IPlatformConfig;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import io.tebex.sdk.request.response.ServerInformation;
import io.tebex.sdk.triage.EnumEventLevel;
import io.tebex.sdk.triage.PluginEvent;
import io.tebex.sdk.util.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static io.tebex.sdk.util.ResourceUtil.getBundledFile;

/**
 * BasePlatform class represents the foundational platform for basic Tebex functionality.
 */
public abstract class BasePluginPlatform implements PluginPlatform {
    public final int MAX_COMMANDS_PER_BATCH = 3;

    protected SDK sdk;
    protected ServerPlatformConfig config;
    protected YamlDocument configYaml;

    protected boolean setup;
    protected PlaceholderManager placeholderManager;
    protected Map<Object, Integer> queuedPlayers;

    protected ServerInformation storeInformation;
    protected List<Category> storeCategories = new ArrayList<>();
    protected List<ServerEvent> serverEvents = new ArrayList<>();

    private final ArrayList<PluginEvent> PLUGIN_EVENTS = new ArrayList<>();
    private final Set<Integer> processingCommandIds = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Integer> processingPlayerIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    /**
     * Checks if the configured store is Geyser/Offline
     *
     * @return Whether the store is an Offline/Geyser-type webstore
     */
    public final boolean isGeyser() {
        if (!isSetup()) return false;

        if (getStoreType() == null || getStoreType().isEmpty()) {
            return false;
        }

        return getStoreType().contains("Offline/Geyser");
    }

    public final void initStore() {
        sdk = new SDK(this, config.getSecretKey());
        placeholderManager = new PlaceholderManager();
        queuedPlayers = Maps.newConcurrentMap();
        storeCategories = new ArrayList<>();
        serverEvents = new ArrayList<>();
        placeholderManager.register(new UuidPlaceholder(placeholderManager));

        if (getPlatformConfig().getSecretKey() != null && !getPlatformConfig().getSecretKey().isEmpty()) {
            getSDK().getServerInformation().thenAccept(serverInformation -> {
                ServerInformation.Server server = serverInformation.getServer();
                ServerInformation.Store store = serverInformation.getStore();

                info(String.format("Connected to %s - %s server.", server.getName(), store.getGameType()));
                createPluginEvent(EnumEventLevel.INFO, "Server init");
                setStoreInfo(serverInformation);
                setSetup(true);
                configure();
                refreshListings();

                // Start the initial check, which is rescheduled per each remote next check
                checkCommandQueue(true);
            }).exceptionally(ex -> {
                Throwable cause = ex.getCause();
                setSetup(false);

                if (cause instanceof ServerNotFoundException) {
                    warning("Failed to connect your server: " + cause.getMessage(), "Please double-check your server key or run the setup command again.");
                    this.halt();
                } else {
                    warning("Failed to retrieve server information. " + cause.getMessage(), "Please double check your server key or run the setup command again.", ex);
                }

                return null;
            });
        } else {
            info("Welcome to Tebex! It seems like this is a new setup.");
            info("To get started, please use the 'tebex secret <key>' command in the console.");
        }
    }

    public final void performCheck() {
        checkCommandQueue(true);
    }

    public final CompletableFuture<String[]> checkCommandQueue(boolean useRemoteNextCheck) {
        CompletableFuture<String[]> forceCheckOutput = new CompletableFuture<>();

        if(!isSetup()) {
            debug("Tebex is not set up. Skipping check.");
            executeAsyncLater(this::performCheck, 1, TimeUnit.MINUTES);
            return forceCheckOutput;
        }

        debug("Checking for due players...");

        getSDK().getDuePlayers().whenComplete((duePlayersResponse, ex) -> {
            ArrayList<String> output = new ArrayList<>();
            if (ex != null) {
                if (ex.getMessage().contains("429")) { // handling for rate limits
                    warning("Failed to get due players: Rate Limit", "We will try again after 5 minutes.", ex);
                    output.add("Failed to get due players: Rate Limit. We will try again after 5 minutes.");
                    executeAsyncLater(this::performCheck, 5, TimeUnit.MINUTES);
                } else if (ex.getMessage().contains("403")) {
                    warning("Failed to get due players: Forbidden", "Please check your secret key and run `/tebex.forcecheck` to try again. We will wait 30 minutes before trying again.", ex);
                    output.add("Failed to get due players: Forbidden. Please check your secret key and run `/tebex.forcecheck` to try again. We will wait 30 minutes before trying again.");
                    executeAsyncLater(this::performCheck, 30, TimeUnit.MINUTES);
                } else { // unexpected status code
                    warning("Failed to get due players: " + ex.getMessage(), "We will try again at the next due player check.", ex);
                    output.add("Failed to get due players: '" + ex.getMessage() + "'. We will try again at the next due player check.");
                    executeAsyncLater(this::performCheck, 1, TimeUnit.MINUTES);
                }
                forceCheckOutput.complete(output.toArray(new String[0]));
                return;
            }

            if (useRemoteNextCheck) {
                int nextCheck = duePlayersResponse == null ? 60 : duePlayersResponse.getNextCheck();
                executeAsyncLater(this::performCheck, nextCheck, TimeUnit.SECONDS);
            }

            List<QueuedPlayer> playerList = duePlayersResponse.getPlayers();
            if(! playerList.isEmpty()) {
                String listMessage = "Found " + playerList.size() + " " + StringUtil.pluralise(playerList.size(), "player", "players") + " with pending commands.";

                for (QueuedPlayer queuedPlayer : playerList) {
                    try {
                        handleOnlineCommands(queuedPlayer);
                    } catch (Exception e) {
                        error("Failed to handle online commands for player '" + queuedPlayer.getName() + "': " + e.getMessage(), e);
                    }
                }
            }

            if(! duePlayersResponse.isExecuteOffline()) return;
            handleOfflineCommands();
        });

        return forceCheckOutput;
    }

    public final void handleOnlineCommands(QueuedPlayer player) {
        if(! isSetup()) return;

        if (!processingPlayerIds.add(player.getId())) {
            debug("Already processing commands for player '" + player.getName() + "'. Skipping duplicate request.");
            return;
        }

        debug("Processing online commands for player '" + player.getName() + "'...");
        Object playerId = getPlayerId(player.getName(), UUIDUtil.mojangIdToJavaId(player.getUuid()));
        if(!isPlayerOnline(playerId)) {
            debug("Player " + player.getName() + " has online commands but is not connected. Skipping.");
            getQueuedPlayers().put(playerId, player.getId()); // will cause commands to be processed when player connects
            processingPlayerIds.remove(player.getId());
            return;
        }

        getSDK().getOnlineCommands(player).thenAccept(onlineCommands -> {
            if(onlineCommands.isEmpty()) {
                debug("No commands found for " + player.getName() + ".");
                processingPlayerIds.remove(player.getId());
                return;
            }

            debug("Found " + onlineCommands.size() + " online " + StringUtil.pluralise(onlineCommands.size(), "command") + ".");
            processOnlineCommands(player.getName(), playerId, onlineCommands);
            processingPlayerIds.remove(player.getId());
        }).exceptionally(ex -> {
            warning("Failed to get online commands: " + ex.getMessage(), "We will try again at the next due player check.", ex);
            processingPlayerIds.remove(player.getId());
            return null;
        });
    }

    /**
     * Selects the appropriate player ID for a player based on platform configuration.
     * @param name The name of the player.
     * @param uuid The UUID of the player.
     * @return The player ID to use.
     */
    @NotNull
    public final Object getPlayerId(String name, UUID uuid) {
        // online mode uses uuids while offline mode uses usernames. public final to the name if we ever fail to have an uuid
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
    public final void processOnlineCommands(String playerName, Object playerId, List<QueuedCommand> commands) {
        if(! isSetup()) return;

        List<Integer> completedCommands = new ArrayList<>();
        boolean hasInventorySpace = true;
        for (QueuedCommand command : commands) {
            if (!processingCommandIds.add(command.getId())) {
                continue;
            }

            int freeSlots = getFreeSlots(playerId);
            if(freeSlots < command.getRequiredSlots()) {
                info(String.format("Skipping command '%s' for player '%s' due to no inventory space. Free slots: %d. Slots required: %d", command.getParsedCommand(), playerName, freeSlots, command.getRequiredSlots()));
                hasInventorySpace = false;
                processingCommandIds.remove(command.getId());
                continue;
            }

            final Runnable commandRunnable = () -> {
                info(String.format("Dispatching command '%s' for player '%s'", command.getParsedCommand(), playerName));
                CommandResult commandResult = dispatchCommand(command.getParsedCommand());

                // report whether the command succeeded or failed
                if (!commandResult.isSuccess()) {
                    String extraInfo = "";
                    Throwable commandException = commandResult.getException();
                    if (commandResult.getMessage() != null && !commandResult.getMessage().isEmpty()) {
                        extraInfo = commandResult.getMessage();
                    }
                    if (commandException != null) {
                        extraInfo = commandResult.getException().getMessage();
                    }

                    if (extraInfo.isEmpty()) {
                        extraInfo = "No further information";
                    }

                    String solution = String.format("Manually try `%s` for player %s. Check that the command syntax is correct.", command.getParsedCommand(), playerName);
                    if (command.getPayment() != 0) {
                        solution += " Re-run this command at https://creator.tebex.io/payments/" + command.getPayment();
                    }
                    warning("Command failed to execute: " + extraInfo, solution);
                }
            };
            if (command.getDelay() > 0) {
                executeBlockingLater(commandRunnable, command.getDelay(), TimeUnit.SECONDS);
            } else {
                executeBlocking(commandRunnable);
            }

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

    public final void handleOfflineCommands() {
        if(! isSetup()) return;

        getSDK().getOfflineCommands().thenAccept(offlineData -> {
            if(offlineData.getCommands().isEmpty()) {
                return;
            }

            List<Integer> completedCommands = new ArrayList<>();
            for (QueuedCommand command : offlineData.getCommands()) {
                if (!processingCommandIds.add(command.getId())) {
                    continue;
                }

                Object pid = getPlayerId(command.getPlayer().getName(), UUIDUtil.mojangIdToJavaId(command.getPlayer().getUuid()));
                if (isPlayerOnline(pid)) {
                    processingCommandIds.remove(command.getId());
                    continue;
                }

                final Runnable commandRunnable = () -> {
                    info(String.format("Dispatching offline command '%s' for player '%s'.", command.getParsedCommand(), command.getPlayer().getName()));
                    CommandResult offlineCommandResult = dispatchCommand(command.getParsedCommand());

                    // report whether the offline command succeeded or failed
                    if (!offlineCommandResult.isSuccess()) {
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
                };
                if (command.getDelay() > 0) {
                    executeBlockingLater(commandRunnable, command.getDelay(), TimeUnit.SECONDS);
                } else {
                    executeBlocking(commandRunnable);
                }

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

    public final void deleteCompletedCommands(List<Integer> completedCommands) {
        List<Integer> toRemove = new ArrayList<>(completedCommands);
        getSDK().deleteCommands(completedCommands).thenRun(() -> {
            toRemove.forEach(processingCommandIds::remove);
            completedCommands.clear();
        }).exceptionally(ex -> {
            error("Failed to delete commands: " + ex.getMessage(), ex);
            return null;
        });
    }

    /**
     * Converts the version string into a version number.
     *
     * @return The version number.
     */
    public final int getVersionNumber() {
        return Integer.parseInt(getPluginVersion().replace(".", ""));
    }

    /**
     * Logs an informational message to the console.
     *
     * @param message The message to log.
     */
    public final void info(String message) {
        log(Level.INFO, message);
    }

    /**
     * Logs a warning message to the console. A "warning" is due to a problem that either the user can solve, or a
     * problem that can resolve itself later. All warnings must have solutions provided.
     * <p>
     * ex.)
     * @param message The message to log.
     * @param solution User-friendly description of how to resolve the problem.
     */
    public final void warning(String message, String solution) {
        log(Level.WARNING, message);
        log(Level.WARNING, "- " + solution);
        createPluginEvent(EnumEventLevel.WARNING, message);
    }

    public final void warning(String message, String solution, Throwable t) {
        log(Level.WARNING, message);
        log(Level.WARNING, "- " + solution);
        createPluginEvent(EnumEventLevel.WARNING, message);
    }

    public final void error(String message) {
        log(Level.SEVERE, message);
        createPluginEvent(EnumEventLevel.ERROR, message);
    }

    public final void error(String message, Throwable t) {
        log(Level.SEVERE, message);
        createPluginEvent(EnumEventLevel.ERROR, message, t);
    }

    /**
     * Logs a debug message to the console if debugging is enabled in the platform configuration.
     *
     * @param message The message to log.
     */
    public final void debug(String message) {
        if (getPlatformConfig() != null && !getPlatformConfig().isVerbose()) return;
        info("[DEBUG] " + message);
    }

    // Create and update the file
    public final YamlDocument initPlatformConfig() throws IOException {
        return YamlDocument.create(getBundledFile(this, getRunningDirectory(), "config.yml"));
    }

    /**
     * Loads the server platform configuration from the file.
     *
     * @param configFile The configuration file.
     * @return The PlatformConfig instance representing the loaded configuration.
     */
    public final ServerPlatformConfig loadServerPlatformConfig(YamlDocument configFile) {
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

    public final void refreshListings() {
        getSDK().getListing().thenAccept(this::setStoreCategories);
    }

    public final void clearPluginEvents() {
        this.PLUGIN_EVENTS.clear();
    }

    public final void createPluginEvent(EnumEventLevel level, String message) {
        if (!getPlatformConfig().isAutoReportEnabled()) {
            return;
        }

        PluginEvent event = new PluginEvent(this, level, message);
        if (isSetup()) {
            event = event.onStore(getStore()).onServer(getStoreServer());
        }
        PLUGIN_EVENTS.add(event);
    }

    public final void createPluginEvent(EnumEventLevel level, String message, Throwable e) {
        if (!getPlatformConfig().isAutoReportEnabled()) {
            log(Level.SEVERE, e.getMessage());
            return;
        }
        PluginEvent event = new PluginEvent(this, level, message).withTrace(e);
        PLUGIN_EVENTS.add(event);
    }

    @Override
    public final SDK getSDK() {
        return sdk;
    }

    /**
     * Checks if the platform is set up and ready to use.
     *
     * @return True if the platform is set up, false otherwise.
     */
    public final boolean isSetup() {
        return setup;
    }

    /**
     * Sets whether the platform is set up and ready to use.
     */
    public final void setSetup(boolean setup) {
        this.setup = setup;
    }

    /**
     * Halts the platform and stops any ongoing tasks.
     */
    public final void halt() {
        this.setup = false;
    }

    public final PlaceholderManager getPlaceholderManager() {
        return placeholderManager;
    }

    public final Map<Object, Integer> getQueuedPlayers() {
        return queuedPlayers;
    }

    public final void setStoreInfo(ServerInformation info) {
        this.storeInformation = info;
    }

    public final ServerInformation getStoreInformation() {
        return this.storeInformation;
    }

    public final void setStoreCategories(List<Category> categories) {
        this.storeCategories = categories;
    }

    public final List<Category> getStoreCategories() {
        return this.storeCategories;
    }

    public final ServerInformation.Server getStoreServer() {
        return this.storeInformation.getServer();
    }

    public final ServerInformation.Store getStore() {
        return this.storeInformation.getStore();
    }

    public final String getStoreType() {
        return storeInformation == null ? "" : storeInformation.getStore().getGameType();
    }

    /**
     * Gets the current platform configuration.
     *
     * @return The PlatformConfig instance representing the current configuration.
     */
    public IPlatformConfig getPlatformConfig() {
        return config;
    }

    public void executeAsync(Runnable runnable) {
        Multithreading.runAsync(runnable);
    }

    public void executeAsyncLater(Runnable runnable, long time, TimeUnit unit) {
        Multithreading.executeAsyncLater(runnable, time, unit);
    }

    public void executeBlocking(Runnable runnable) {
        try {
            Multithreading.executeBlocking(runnable);
        } catch (InterruptedException | ExecutionException e) {
            error("Failed to execute blocking task: " + e.getMessage(), e);
        }
    }

    public void executeBlockingLater(Runnable runnable, long time, TimeUnit unit) {
        TickScheduler.scheduleLater(runnable, time, unit);
    }

    public final List<ServerEvent> getJoinEvents() {
        return serverEvents;
    }

    public void createJoinEvent(String uuid, String username, String ip) {
        serverEvents.add(new ServerEvent(uuid, username, ip, EnumServerEventType.JOIN));
    }

    public final void configure() {
        setup = true;
        sdk.sendTelemetry();
    }

    public ArrayList<PluginEvent> getPluginEvents() {
        return PLUGIN_EVENTS;
    }

    public void loadPlatformConfig() {
        try {
            configYaml = initPlatformConfig();
            config = loadServerPlatformConfig(configYaml);
            String envKey = System.getenv("TEBEX_SECRET_KEY");
            if (envKey != null && !envKey.isEmpty()) {
                debug("Using secret key from environment variable");
                config.setSecretKey(envKey);
            }

            if (config.getYamlDocument() == null) {
                error("YamlDocument is null after loading config");
            }
        } catch (IOException e) {
            warning("Failed to load configuration: " + e.getMessage(),
                    "Check that your configuration is valid and in the proper format and reload the plugin. You may delete `Tebex/config.yml` and a new configuration will be generated.");
        }

    }

    @Override
    public void reloadConfig() {
        loadPlatformConfig();
    }

    @Override
    public void saveConfig(IPlatformConfig platformConfig) {
        try {
            config.setSecretKey(platformConfig.getSecretKey());
            config.setVerbose(platformConfig.isVerbose());
            this.config = (ServerPlatformConfig) platformConfig;
            YamlDocument yamlDoc = this.config.getYamlDocument();
            if (yamlDoc == null) {
                error("YamlDocument is null when trying to save config!?");
                return;
            }

            // ensure actual value is set in the document
            yamlDoc.set("server.secret-key", this.config.getSecretKey());
            yamlDoc.set("verbose", this.config.isVerbose());

            yamlDoc.save(); // Save the updated YAML document
        } catch (Exception e) {
            error("Failed to save configuration: " + e.getMessage(), e);
        }
    }

    public void clearSelectedPluginEvents(List<ServerEvent> events) {
        serverEvents.removeAll(events);
    }

    public void setServerInformation(ServerInformation info) {
        this.storeInformation = info;
    }
}
