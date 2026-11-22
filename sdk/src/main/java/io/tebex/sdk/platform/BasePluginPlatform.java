package io.tebex.sdk.platform;

import com.google.common.collect.Maps;
import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.sdk.SDK;
import io.tebex.sdk.exception.InvalidSecretKeyException;
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
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.util.LinkedPlayer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;

import static io.tebex.sdk.util.ResourceUtil.getBundledFile;

/**
 * BasePlatform class represents the foundational platform for basic Tebex functionality.
 */
public abstract class BasePluginPlatform implements PluginPlatform {

    public final int MAX_COMMANDS_PER_BATCH = 3;

    private final long INITIAL_STORE_RETRY_SECONDS = 15;
    private final long MAX_STORE_RETRY_SECONDS = 300;
    private final long COMMAND_SCHEDULED = -1;
    private final long COMMAND_NEEDS_DELETE = -2;
    private final long COMMAND_DELETING = -3;
    private final long DELETED_COMMAND_RETENTION_MILLIS = TimeUnit.MINUTES.toMillis(5);

    protected SDK sdk;
    protected ServerPlatformConfig config;
    protected YamlDocument configYaml;

    protected volatile boolean setup;
    protected PlaceholderManager placeholderManager;
    protected Map<Object, Integer> queuedPlayers;

    protected ServerInformation storeInformation;
    protected List<Category> storeCategories = new ArrayList<>();
    protected List<ServerEvent> serverEvents = Collections.synchronizedList(new ArrayList<>());

    private final ArrayList<PluginEvent> PLUGIN_EVENTS = new ArrayList<>();
    private final AtomicBoolean commandCheckInProgress = new AtomicBoolean(false);
    private final AtomicBoolean floodgateWarningLogged = new AtomicBoolean(false);
    private final Map<String, UUID> resolvedFloodgateIds = Maps.newConcurrentMap();
    private final Map<String, Map<Integer, Long>> commandStatesByKey = Maps.newConcurrentMap();
    private final AtomicBoolean queueCheckScheduled = new AtomicBoolean(false);
    private long storeInitGeneration;

    /**
     * Checks if the configured store is Geyser/Offline
     *
     * @return Whether the store is an Offline/Geyser-type webstore
     */
    public final boolean isGeyser() {
        if (!isSetup()) return false;

        String storeType = getStoreType();
        if (storeType == null || storeType.isEmpty()) {
            return false;
        }

        return storeType.toLowerCase(Locale.ROOT).contains("geyser");
    }

    public final synchronized void initStore() {
        long generation = ++storeInitGeneration;
        String secretKey = config.getSecretKey();

        setSetup(false);
        sdk = new SDK(this, secretKey);
        placeholderManager = new PlaceholderManager();
        queuedPlayers = Maps.newConcurrentMap();
        resolvedFloodgateIds.clear();
        storeCategories = new ArrayList<>();
        serverEvents = Collections.synchronizedList(new ArrayList<>());
        placeholderManager.register(new UuidPlaceholder(placeholderManager));

        if (secretKey == null || secretKey.isEmpty()) {
            info("Welcome to Tebex! It seems like this is a new setup.");
            info("To get started, please use the 'tebex secret <key>' command in the console.");
            return;
        }

        connectStore(generation, secretKey, INITIAL_STORE_RETRY_SECONDS);
    }

    private synchronized void connectStore(long generation, String secretKey, long retrySeconds) {
        if (generation != storeInitGeneration || !secretKey.equals(sdk.getSecretKey())) {
            return;
        }

        sdk.getServerInformation().whenComplete((serverInformation, error) ->
                completeStoreInit(generation, secretKey, retrySeconds, serverInformation, error));
    }

    private synchronized void completeStoreInit(long generation, String secretKey, long retrySeconds, ServerInformation serverInformation, Throwable error) {

        // Ignore responses from a setup attempt superseded by a new one or by halt().
        if (generation != storeInitGeneration || !secretKey.equals(sdk.getSecretKey())) return;

        if (error != null) {
            Throwable cause = error instanceof CompletionException && error.getCause() != null ? error.getCause() : error;
            if (cause instanceof ServerNotFoundException) {
                warning("Failed to connect your server: " + cause.getMessage(), "Please double-check your server key or run the setup command again.");
                halt();
            } else if (cause instanceof InvalidSecretKeyException) {
                warning(cause.getMessage(), "Check the key in your configuration or run the setup command again.");
            } else if (cause instanceof IOException) {
                warning("Failed to retrieve server information. " + cause.getMessage(), "Retrying in " + retrySeconds + " seconds.", error);

                long nextDelay = Math.min(retrySeconds * 2, MAX_STORE_RETRY_SECONDS);
                executeAsyncLater(() -> connectStore(generation, secretKey, nextDelay), retrySeconds, TimeUnit.SECONDS);
            } else {
                warning("Failed to retrieve server information. " + cause.getMessage(), "Run the setup command again to retry.", error);
            }
            return;
        }

        ServerInformation.Server server = serverInformation.getServer();
        ServerInformation.Store store = serverInformation.getStore();

        info(String.format("Connected to %s - %s server.", server.getName(), store.getGameType()));
        createPluginEvent(EnumEventLevel.INFO, "Server init");
        setStoreInfo(serverInformation);
        setSetup(true);
        configure();
        refreshListings();
        checkCommandQueue(true);
    }

    public final void performCheck() {
        checkCommandQueue(true);
    }

    public final CompletableFuture<String[]> checkCommandQueue(boolean useRemoteNextCheck) {
        if (!isSetup()) {
            debug("Tebex is not set up. Skipping check.");
            scheduleQueueCheck(useRemoteNextCheck, 60);
            return CompletableFuture.completedFuture(new String[]{"Tebex is not set up."});
        }

        if (!commandCheckInProgress.compareAndSet(false, true)) {
            debug("Command queue check already in progress.");
            scheduleQueueCheck(useRemoteNextCheck, 60);
            return CompletableFuture.completedFuture(new String[]{"A command queue check is already in progress."});
        }

        debug("Checking for due players...");
        long[] nextCheckSeconds = {60};
        getQueuedPlayers().clear();

        SDK queueSdk = getSDK();
        String queueKey = queueSdk.getSecretKey();

        return CompletableFuture.completedFuture(null).thenCompose(ignored -> {
            retryCommandDeletes(queueKey);
            return queueSdk.getDuePlayers();
        }).thenCompose(duePlayersResponse -> {
            if (!isCurrentQueueConnection(queueSdk, queueKey)) return CompletableFuture.completedFuture(new String[]{"The store connection changed during the check."});

            nextCheckSeconds[0] = duePlayersResponse.getNextCheck();
            List<CompletableFuture<Void>> onlineCommandFutures = new ArrayList<>();

            for (QueuedPlayer queuedPlayer : duePlayersResponse.getPlayers()) {
                try {
                    onlineCommandFutures.add(handleOnlineCommands(queuedPlayer, queueSdk, queueKey));
                } catch (Exception e) {
                    error("Failed to handle online commands for player '" + queuedPlayer.getName() + "': " + e.getMessage(), e);
                    CompletableFuture<Void> failedFetch = new CompletableFuture<>();
                    failedFetch.completeExceptionally(e);
                    onlineCommandFutures.add(failedFetch);
                }
            }

            // Hold the guard until the online and offline fetches have both finished.
            return CompletableFuture.allOf(onlineCommandFutures.toArray(new CompletableFuture<?>[0]))
                    .handle((ignored, onlineFailure) -> onlineFailure)
                    .thenCompose(onlineFailure -> {
                        if (!isCurrentQueueConnection(queueSdk, queueKey)) return CompletableFuture.completedFuture(new String[]{"The store connection changed during the check."});

                        CompletableFuture<Void> offlineFetch = duePlayersResponse.isExecuteOffline() ? fetchOfflineCommands(queueSdk, queueKey) : CompletableFuture.completedFuture(null);
                        return offlineFetch.handle((ignored, offlineFailure) -> {
                            if (onlineFailure == null && offlineFailure == null) return new String[0];

                            String message = "Some Tebex commands could not be fetched.";
                            Throwable cause = onlineFailure != null ? onlineFailure : offlineFailure;
                            warning(message + " " + cause.getMessage(), "Check the connection to Tebex and try again.", cause);
                            return new String[]{message + " See console for details."};
                        });
                    });
        }).exceptionally(ex -> {
            Throwable cause = ex instanceof CompletionException && ex.getCause() != null ? ex.getCause() : ex;
            String message = cause.getMessage();

            // Match the SDK's API status message. A proxy error can also contain "403".
            if ("Unexpected status code (429)".equals(message)) {
                nextCheckSeconds[0] = 300;
            } else if ("Unexpected status code (403)".equals(message)) {
                nextCheckSeconds[0] = 1800;
            }

            String failure = "Command queue check failed: " + message;
            warning(failure, useRemoteNextCheck ? "Retrying in " + nextCheckSeconds[0] + " seconds." : "See console for details.", ex);
            return new String[]{failure};
        }).whenComplete((result, failure) -> {
            commandCheckInProgress.set(false);
            scheduleQueueCheck(useRemoteNextCheck, nextCheckSeconds[0]);
        });
    }

    private void scheduleQueueCheck(boolean regularCheck, long delaySeconds) {
        if (!regularCheck || !queueCheckScheduled.compareAndSet(false, true)) return;

        executeAsyncLater(() -> {
            queueCheckScheduled.set(false);
            performCheck();
        }, delaySeconds, TimeUnit.SECONDS);
    }

    private boolean isCurrentQueueKey(String key) {
        return isSetup() && key != null && key.equals(getSDK().getSecretKey());
    }

    private boolean isCurrentQueueConnection(SDK queueSdk, String queueKey) {
        return queueSdk == getSDK() && isCurrentQueueKey(queueKey);
    }

    public final CompletableFuture<Void> handleOnlineCommands(QueuedPlayer player) {
        if (!isSetup()) return CompletableFuture.completedFuture(null);

        SDK queueSdk = getSDK();
        return handleOnlineCommands(player, queueSdk, queueSdk.getSecretKey())
                .exceptionally(ex -> {
                    warning("Failed to get online commands: " + ex.getMessage(), "We will try again at the next due player check.", ex);
                    return null;
                });
    }

    private CompletableFuture<Void> handleOnlineCommands(QueuedPlayer player, SDK queueSdk, String queueKey) {
        if (!isCurrentQueueConnection(queueSdk, queueKey)) return CompletableFuture.completedFuture(null);

        debug("Processing online commands for player '" + player.getName() + "'...");
        Object playerId = getPlayerId(player.getName(), UUIDUtil.mojangIdToJavaId(player.getUuid()));
        if (!canProcessOnlineCommands(player, playerId)) {
            getQueuedPlayers().put(playerId, player.getId()); // will cause commands to be processed when player connects
            return CompletableFuture.completedFuture(null);
        }

        return queueSdk.getOnlineCommands(player).thenAccept(onlineCommands -> {
            if (!isCurrentQueueConnection(queueSdk, queueKey)) return;
            if (onlineCommands.isEmpty()) {
                debug("No commands found for " + player.getName() + ".");
                return;
            }

            debug("Found " + onlineCommands.size() + " online " + StringUtil.pluralise(onlineCommands.size(), "command") + ".");
            processOnlineCommands(player, playerId, onlineCommands, queueKey);
        });
    }

    private boolean canProcessOnlineCommands(QueuedPlayer player, Object playerId) {
        if (isOnlineMode() && !isGeyser() && !player.hasUuid()) {
            debug("Player " + player.getName() + " has online commands but no UUID was provided for an online-mode store. Skipping.");
            return false;
        }

        if (!isPlayerOnline(playerId)) {
            debug("Player " + player.getName() + " has online commands but is not connected. Skipping.");
            return false;
        }

        return true;
    }

    /**
     * Selects the appropriate player ID for a player based on platform configuration.
     * @param name The name of the player.
     * @param uuid The UUID of the player.
     * @return The player ID to use.
     */
    @NotNull
    public final Object getPlayerId(String name, UUID uuid) {
        // Geyser/offline stores and missing UUIDs must use usernames for consistent matching.
        boolean useUuid = isOnlineMode() && !isGeyser() && uuid != null && !UUIDUtil.EMPTY_UUID.equals(uuid);
        if (useUuid) {
            return uuid;
        }

        return (name == null) ? "" : name;
    }

    @Override
    public String resolveCommandPlayerId(QueuedPlayer player) {
        if (player == null) {
            return "";
        }

        if (player.hasUuid()) {
            return player.getUuid();
        }

        if (!player.hasXuid()) {
            return player.getDefaultCommandIdentifier();
        }

        UUID resolvedUuid = resolvedFloodgateIds.computeIfAbsent(player.getXuid(), ignored -> resolveFloodgateUniqueId(player));
        if (resolvedUuid != null && !UUIDUtil.EMPTY_UUID.equals(resolvedUuid)) {
            return resolvedUuid.toString();
        }

        return player.getDefaultCommandIdentifier();
    }

    @Override
    public void sendCheckoutLink(String playerName, String checkoutUrl) {
        if (isOnlineFloodgatePlayer(playerName)) {
            sendPlayerMessage(playerName, "Checkout started! Type this link into your browser: " + checkoutUrl);
            return;
        }

        PluginPlatform.super.sendCheckoutLink(playerName, checkoutUrl);
    }

    private UUID resolveFloodgateUniqueId(QueuedPlayer player) {
        if (!isGeyser()) {
            return null;
        }

        Long xuid = player.getXuidAsLong();
        if (xuid == null) {
            debug("Unable to parse Bedrock XUID '" + player.getXuid() + "' for player '" + player.getName() + "'.");
            return null;
        }

        try {
            FloodgateApi api = FloodgateApi.getInstance();
            UUID bedrockId = api.createJavaPlayerId(xuid);

            if (bedrockId == null || UUIDUtil.EMPTY_UUID.equals(bedrockId)) {
                return null;
            }

            FloodgatePlayer onlinePlayer = api.getPlayer(bedrockId);
            if (onlinePlayer != null && onlinePlayer.getCorrectUniqueId() != null) {
                return onlinePlayer.getCorrectUniqueId();
            }

            if (api.getPlayerLink() != null) {
                try {
                    LinkedPlayer linkedPlayer = api.getPlayerLink().getLinkedPlayer(bedrockId).get(2, TimeUnit.SECONDS);
                    if (linkedPlayer != null && linkedPlayer.getJavaUniqueId() != null) {
                        return linkedPlayer.getJavaUniqueId();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    debug("Interrupted while resolving Floodgate UUID for player '" + player.getName() + "'.");
                } catch (ExecutionException | TimeoutException e) {
                    debug("Failed to resolve Floodgate link data for player '" + player.getName() + "': " + e.getMessage());
                }
            }

            return bedrockId;
        } catch (IllegalStateException | NoClassDefFoundError e) {
            warnMissingFloodgateApi();
        }

        return null;
    }

    private boolean isOnlineFloodgatePlayer(String playerName) {
        UUID playerUniqueId = getPlayerUniqueId(playerName);
        if (playerUniqueId == null) {
            return false;
        }

        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(playerUniqueId);
        } catch (IllegalStateException | NoClassDefFoundError e) {
            warnMissingFloodgateApi();
            return false;
        }
    }

    private void warnMissingFloodgateApi() {
        if (!floodgateWarningLogged.compareAndSet(false, true)) {
            return;
        }

        warning(
                "Received a Bedrock XUID for command placeholder resolution, but the Floodgate API is unavailable.",
                "Install Floodgate on the same server or proxy running Tebex so {id}/{uuid} placeholders can be resolved for Bedrock players."
        );
    }

    /**
     * Checks that a queued command is safe to dispatch, reporting blank commands as errors.
     * <p>
     * A blank command cannot be executed by any platform's command dispatcher, so it is reported to the console and
     * the plugin logs instead of being dispatched. It is deliberately left in the queue, meaning it is reported again
     * on each queue check until the package is corrected in the webstore.
     *
     * @param command The queued command.
     * @param parsedCommand The command after its placeholders have been parsed.
     * @param playerName The name of the player the command belongs to.
     * @return true if the command should be dispatched.
     */
    private boolean isDispatchable(QueuedCommand command, String parsedCommand, String playerName) {
        if (parsedCommand != null && !parsedCommand.trim().isEmpty()) return true;

        StringBuilder message = new StringBuilder(String.format(
                "Command #%d for player '%s' is blank and cannot be executed. Check the commands configured on this package in your webstore.",
                command.getId(), playerName
        ));

        if (command.getPackageId() != null && command.getPackageId() != 0) {
            message.append(" Package: ").append(command.getPackageId()).append('.');
        }

        if (command.getPayment() != null && command.getPayment() != 0) {
            message.append(" Payment: https://creator.tebex.io/payments/").append(command.getPayment());
        }

        error(message.toString());

        return false;
    }

    /**
     * Processes the online commands for a player.
     *
     * @param player The queued player.
     * @param playerId The Unique Identifier of the player.
     * @param commands The commands to process.
     */
    public final void processOnlineCommands(QueuedPlayer player, Object playerId, List<QueuedCommand> commands) {
        processOnlineCommands(player, playerId, commands, getSDK().getSecretKey());
    }

    private void processOnlineCommands(QueuedPlayer player, Object playerId, List<QueuedCommand> commands, String queueKey) {
        if (!isCurrentQueueKey(queueKey)) return;

        // The existing body continues here, starting with String playerName.
        if (!isSetup()) return;

        String playerName = player.getName();
        boolean hasInventorySpace = true;
        for (QueuedCommand command : commands) {
            String parsedCommand = command.getParsedCommand(this);
            if (!isDispatchable(command, parsedCommand, playerName)) continue;

            int freeSlots = getFreeSlots(playerId);
            if (freeSlots < command.getRequiredSlots()) {
                info(String.format("Skipping command '%s' for player '%s' due to no inventory space. Free slots: %d. Slots required: %d", parsedCommand, playerName, freeSlots, command.getRequiredSlots()));
                hasInventorySpace = false;
                continue;
            }

            scheduleQueuedCommand(queueKey, command, markDispatchAttempted -> {
                if (!canProcessOnlineCommands(player, playerId)) {
                    info(String.format("Skipping command '%s' for player '%s' because they are no longer verified online", parsedCommand, playerName));
                    return;
                }

                info(String.format("Dispatching command '%s' for player '%s'", parsedCommand, playerName));
                markDispatchAttempted.run();
                CommandResult commandResult = dispatchCommand(parsedCommand);

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

                    String solution = String.format("Manually try `%s` for player %s. Check that the command syntax is correct.", parsedCommand, playerName);
                    if (command.getPayment() != 0) {
                        solution += " Re-run this command at https://creator.tebex.io/payments/" + command.getPayment();
                    }
                    warning("Command failed to execute: " + extraInfo, solution);
                }

                // Mark online commands completed only after their scheduled runnable has verified the player and dispatched.
            });
        }

        if (!hasInventorySpace) return;
        getQueuedPlayers().remove(playerId);
    }

    public final void handleOfflineCommands() {
        if (!isSetup()) return;

        SDK queueSdk = getSDK();
        fetchOfflineCommands(queueSdk, queueSdk.getSecretKey())
                .exceptionally(ex -> {
                    warning("Failed to retrieve offline commands: " + ex.getMessage(), "We will try again at the next due player check.", ex);
                    return null;
                });
    }

    private CompletableFuture<Void> fetchOfflineCommands(SDK queueSdk, String queueKey) {
        if (!isCurrentQueueConnection(queueSdk, queueKey)) return CompletableFuture.completedFuture(null);

        return queueSdk.getOfflineCommands().thenAccept(offlineData -> {
            if (!isCurrentQueueConnection(queueSdk, queueKey)) return;
            if (offlineData.getCommands().isEmpty()) return;

            for (QueuedCommand command : offlineData.getCommands()) {
                String parsedCommand = command.getParsedCommand(this);
                if (!isDispatchable(command, parsedCommand, command.getPlayer().getName())) continue;

                scheduleQueuedCommand(queueKey, command, markDispatchAttempted -> {
                    info(String.format("Dispatching offline command '%s' for player '%s'.", parsedCommand, command.getPlayer().getName()));
                    markDispatchAttempted.run();
                    CommandResult offlineCommandResult = dispatchCommand(parsedCommand);

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
                        warning(String.format("Command `%s` failed to execute: %s", parsedCommand, extraInfo), solution);
                    }
                });
            }
        });
    }

    private void scheduleQueuedCommand(String key, QueuedCommand command, Consumer<Runnable> dispatch) {
        if (!isCurrentQueueKey(key)) return;

        Map<Integer, Long> states = commandStatesByKey.computeIfAbsent(key, ignored -> Maps.newConcurrentMap());
        int id = command.getId();

        // Claim the ID before scheduling so later fetches skip delayed commands.
        if (states.putIfAbsent(id, COMMAND_SCHEDULED) != null) return;

        Runnable task = () -> {
            if (!isCurrentQueueKey(key)) {
                states.remove(id, COMMAND_SCHEDULED);
                return;
            }

            AtomicBoolean dispatchAttempted = new AtomicBoolean(false);
            try {
                dispatch.accept(() -> dispatchAttempted.set(true));
            } finally {
                if (dispatchAttempted.get()) {
                    states.replace(id, COMMAND_SCHEDULED, COMMAND_NEEDS_DELETE);
                    deleteQueuedCommand(key, states, id);
                } else {
                    states.remove(id, COMMAND_SCHEDULED);
                }
            }
        };

        try {
            if (command.getDelay() > 0) {
                executeBlockingLater(task, command.getDelay(), TimeUnit.SECONDS);
            } else {
                executeBlocking(task);
            }
        } catch (RuntimeException e) {
            states.remove(id, COMMAND_SCHEDULED);
            throw e;
        }
    }

    private void retryCommandDeletes(String key) {
        if (!isCurrentQueueKey(key)) return;

        Map<Integer, Long> states = commandStatesByKey.get(key);
        if (states == null) return;

        long now = System.currentTimeMillis();
        states.forEach((id, state) -> {
            if (state == COMMAND_NEEDS_DELETE) {
                deleteQueuedCommand(key, states, id);
            } else if (state >= 0 && state <= now) {
                states.remove(id, state);
            }
        });
    }

    private void deleteQueuedCommand(String key, Map<Integer, Long> states, int id) {
        SDK currentSdk = getSDK();
        if (!isSetup() || !key.equals(currentSdk.getSecretKey()) || !states.replace(id, COMMAND_NEEDS_DELETE, COMMAND_DELETING)) return;

        try {
            currentSdk.deleteCommands(Collections.singletonList(id)).whenComplete((deleted, failure) -> {
                if (failure == null && Boolean.TRUE.equals(deleted)) {
                    // Keep the ID briefly in case an overlapping fetch still lists it.
                    states.replace(id, COMMAND_DELETING, System.currentTimeMillis() + DELETED_COMMAND_RETENTION_MILLIS);
                } else {
                    states.replace(id, COMMAND_DELETING, COMMAND_NEEDS_DELETE);
                    error("Failed to delete command " + id + ". Will retry at the next queue check.", failure);
                }
            });
        } catch (RuntimeException e) {
            states.replace(id, COMMAND_DELETING, COMMAND_NEEDS_DELETE);
            error("Failed to delete command " + id + ". Will retry at the next queue check.", e);
        }
    }

    public final void deleteCompletedCommands(List<Integer> completedCommands) {
        getSDK().deleteCommands(completedCommands).thenRun(completedCommands::clear).exceptionally(ex -> {
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
    public final synchronized void halt() {
        storeInitGeneration++;
        setSetup(false);
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
