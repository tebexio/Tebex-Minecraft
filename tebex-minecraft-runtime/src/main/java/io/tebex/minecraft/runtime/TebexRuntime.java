package io.tebex.minecraft.runtime;

import io.tebex.TXE;
import io.tebex.http.PluginApi;
import io.tebex.minecraft.commands.TebexCommands;
import io.tebex.minecraft.platform.PlatformHost;
import io.tebex.minecraft.platform.PlatformTelemetry;
import io.tebex.minecraft.platform.PlatformType;
import io.tebex.minecraft.platform.config.ServerPlatformConfig;
import io.tebex.minecraft.util.CommandResult;
import io.tebex.model.Category;
import io.tebex.model.PluginEvent;
import io.tebex.model.QueuedPlayer;
import io.tebex.model.ServerInformation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Platform-neutral Tebex connection lifecycle and stable facade for native adapters and commands.
 * Transport behavior belongs to the official SDK; native behavior belongs to {@link PlatformHost}.
 */
public final class TebexRuntime {
  private final PlatformHost host;
  private final SessionFactory sessions;
  private final RuntimeClock clock;
  private final RuntimeConfiguration configuration;
  private final CommandDelivery delivery;
  private final EventReporter events;
  private final PlayerResolver players;
  private final TebexCommands commands;
  private final ScheduledExecutorService worker;
  private final AtomicBoolean started = new AtomicBoolean();
  private final Set<CompletableFuture<?>> outstanding = ConcurrentHashMap.newKeySet();

  private volatile SdkSession sdk;
  private volatile ServerPlatformConfig config;
  private volatile boolean setup;
  private volatile boolean stopped;
  private volatile long generation;
  private volatile long nextRefresh;
  private volatile boolean refreshing;
  private volatile ServerInformation storeInformation;
  private volatile List<Category> storeCategories = Collections.emptyList();

  public TebexRuntime(PlatformHost host) {
    this(host, SdkSession::new, System::currentTimeMillis);
  }

  TebexRuntime(PlatformHost host, SessionFactory sessions, RuntimeClock clock) {
    this.host = Objects.requireNonNull(host, "host");
    this.sessions = Objects.requireNonNull(sessions, "sessions");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.configuration = new RuntimeConfiguration(host, () -> System.getenv("TEBEX_SECRET_KEY"));
    this.delivery = new CommandDelivery(this);
    this.events = new EventReporter(this);
    this.players = new PlayerResolver(host, this::debug, this::warning);
    this.commands = new TebexCommands(this);
    this.worker =
        Executors.newSingleThreadScheduledExecutor(
            runnable -> {
              Thread thread = new Thread(runnable, "tebex-minecraft");
              thread.setDaemon(true);
              return thread;
            });
  }

  public TebexCommands getCommands() {
    return commands;
  }

  public void start() {
    if (sdk == null) sdk = sessions.create("");
    if (started.compareAndSet(false, true))
      worker.scheduleWithFixedDelay(this::tickSafely, 0, 50, TimeUnit.MILLISECONDS);
    if (config != null
        && config.getSecretKey() != null
        && !config.getSecretKey().trim().isEmpty()) {
      connect(config.getSecretKey(), false)
          .exceptionally(
              error -> {
                warning("Failed to connect to Tebex.", "Check the configured secret key.", error);
                return null;
              });
    } else info("Welcome to Tebex! Set your key with /tebex secret <key>.");
  }

  /** Validates a new credential before atomically replacing the active SDK session. */
  public CompletableFuture<ServerInformation> connect(String key, boolean persist) {
    if (stopped) return cancelled("Plugin stopped");
    SdkSession candidate = sessions.create(key);
    final long requestGeneration;
    synchronized (this) {
      requestGeneration = ++generation;
    }
    return track(
        candidate
            .request((api, secret) -> api.getServerInformation(secret))
            .thenApply(this::validateServerInformation)
            .thenCompose(
                information -> adopt(candidate, information, key, persist, requestGeneration))
            .whenComplete(
                (information, error) -> {
                  if (error != null) candidate.close();
                }));
  }

  public CompletableFuture<String[]> checkCommandQueue() {
    return delivery.check();
  }

  public synchronized void refreshListings() {
    if (!isSetup() || refreshing) return;
    refreshing = true;
    nextRefresh = nowMillis() + 300_000L;
    SdkSession connection = sdk;
    connection
        .request((api, secret) -> api.getListing(secret))
        .whenComplete(
            (categories, error) -> {
              if (isCurrent(connection) && error == null)
                storeCategories = Collections.unmodifiableList(new ArrayList<>(categories));
              if (isCurrent(connection)) refreshing = false;
            });
  }

  /** The future completes only after the native command dispatcher finishes. */
  public CompletableFuture<CommandResult> dispatchAsync(String command, Supplier<Boolean> active) {
    CompletableFuture<CommandResult> nativeDispatch = host.dispatchCommandAsync(command, active);
    if (nativeDispatch != null) return track(nativeDispatch);
    CompletableFuture<CommandResult> result = track(new CompletableFuture<>());
    if (stopped) return result;
    executeBlocking(
        () -> {
          try {
            result.complete(active.get() ? host.dispatchCommand(command) : null);
          } catch (Exception error) {
            result.completeExceptionally(error);
          }
        });
    return result;
  }

  public <T> CompletableFuture<T> callPlayer(Object player, Supplier<T> action) {
    CompletableFuture<T> result = track(new CompletableFuture<>());
    if (stopped) return result;
    host.executeForPlayer(
        player,
        () -> {
          try {
            if (stopped) throw new CancellationException("Plugin stopped");
            result.complete(action.get());
          } catch (Exception error) {
            result.completeExceptionally(error);
          }
        },
        () -> result.completeExceptionally(new CancellationException("Player disconnected")));
    return result;
  }

  public void executeForPlayer(Object player, Runnable action, Runnable retired) {
    host.executeForPlayer(player, action, retired);
  }

  public synchronized void shutdown() {
    if (stopped) return;
    stopped = true;
    setup = false;
    generation++;
    if (sdk != null) sdk.close();
    delivery.close();
    worker.shutdownNow();
    outstanding.forEach(
        future -> future.completeExceptionally(new CancellationException("Plugin stopped")));
    events.clear();
  }

  public boolean isStopped() {
    return stopped;
  }

  public boolean isSetup() {
    return setup && !stopped;
  }

  public SdkSession getSession() {
    return sdk;
  }

  /** Runs a Minecraft command/shop operation through the active official SDK client. */
  public <T> CompletableFuture<T> request(
      BiFunction<PluginApi, String, CompletableFuture<T>> operation) {
    SdkSession connection = sdk;
    if (!isCurrent(connection)) return failed("Tebex is not connected");
    return connection.request(operation);
  }

  public List<Category> getStoreCategories() {
    return storeCategories;
  }

  public ServerInformation getStoreInformation() {
    return storeInformation;
  }

  public ServerInformation.Account getAccount() {
    return storeInformation == null ? null : storeInformation.getAccount();
  }

  public ServerInformation.Server getStoreServer() {
    return storeInformation == null ? null : storeInformation.getServer();
  }

  public String getStoreType() {
    return getAccount() == null ? "" : getAccount().getGameType();
  }

  public boolean isGeyser() {
    return getStoreType() != null && getStoreType().toLowerCase(Locale.ROOT).contains("geyser");
  }

  public Object playerId(String name, UUID uuid) {
    return players.playerId(name, uuid, host.isOnlineMode(), isGeyser());
  }

  public String resolveCommandPlayerId(QueuedPlayer player) {
    return players.commandPlayerId(player, isGeyser());
  }

  public void sendCheckoutLink(String name, String url) {
    SdkSession connection = sdk;
    callPlayer(
        name,
        () -> {
          if (isCurrent(connection)) players.sendCheckoutLink(name, url);
          return null;
        });
  }

  public void info(String message) {
    host.log(java.util.logging.Level.INFO, message);
  }

  public void debug(String message) {
    if (config != null && config.isVerbose()) info("[DEBUG] " + message);
  }

  public void warning(String message, String solution) {
    host.log(java.util.logging.Level.WARNING, message + " " + solution);
    createPluginEvent(PluginEvent.Level.WARNING, message);
  }

  public void warning(String message, String solution, Throwable error) {
    Throwable cause = rootCause(error);
    String detail = throwableSummary(cause);
    host.log(
        java.util.logging.Level.WARNING,
        message + " " + solution + (detail.isEmpty() ? "" : " " + detail));
    createPluginEvent(PluginEvent.Level.WARNING, message, cause);
  }

  public void error(String message) {
    error(message, null);
  }

  public void error(String message, Throwable error) {
    host.log(java.util.logging.Level.SEVERE, message);
    createPluginEvent(PluginEvent.Level.ERROR, message, error);
  }

  public String getPluginVersion() {
    return host.getPluginVersion();
  }

  public PlatformType getType() {
    return host.getType();
  }

  public PlatformTelemetry getTelemetry() {
    return host.getTelemetry();
  }

  public boolean isOnlineMode() {
    return host.isOnlineMode();
  }

  public boolean isPlayerOnline(Object player) {
    return host.isPlayerOnline(player);
  }

  public boolean hasPermission(String username, String permission) {
    return host.hasPermission(username, permission);
  }

  public void sendPlayerMessage(String playerName, String message) {
    host.sendPlayerMessage(playerName, message);
  }

  /** Shared /buy access policy; native adapters only convert their command source. */
  public String buyAccessError(String username) {
    if (username == null) return "The buy command cannot be used from the console.";
    if (!host.hasPermission(username, "tebex.buy") && !host.hasPermission(username, "tebex.admin"))
      return "You do not have permission to use the buy command.";
    return isSetup() ? null : "Tebex is not setup yet!";
  }

  public void onPlayerJoin(String name, UUID uuid, String ip) {
    events.recordJoin(uuid.toString(), name, ip);
    if (delivery.hasQueuedPlayer(playerId(name, uuid))) checkCommandQueue();
  }

  public void createPluginEvent(PluginEvent.Level level, String message) {
    createPluginEvent(level, message, null);
  }

  public void createPluginEvent(PluginEvent.Level level, String message, Throwable error) {
    if (!isReportingEnabled()) return;
    PlatformTelemetry telemetry = host.getTelemetry();
    PluginEvent event =
        new PluginEvent(level, message)
            .onStore(getAccount())
            .onServer(getStoreServer())
            .onPlatform(
                telemetry.getServerSoftware(),
                telemetry.getServerVersion(),
                telemetry.getJavaVersion(),
                getPluginVersion());
    if (error != null) event.withTrace(error);
    events.recordPlugin(event);
  }

  public ServerPlatformConfig getConfig() {
    return config;
  }

  public void loadConfig() {
    config = configuration.load();
    syncSdkDebugMode();
  }

  public void reloadConfig() {
    loadConfig();
    if (config.getSecretKey() != null && !config.getSecretKey().isEmpty())
      connect(config.getSecretKey(), false);
  }

  public void saveConfig(ServerPlatformConfig value) {
    config = Objects.requireNonNull(value, "config");
    syncSdkDebugMode();
    configuration.save(config);
  }

  public void migrateLegacyConfig(Properties properties) {
    configuration.migrateLegacy(properties);
  }

  public void executeAsync(Runnable action) {
    if (!stopped) worker.execute(action);
  }

  public void executeAsyncLater(Runnable action, long delay, TimeUnit unit) {
    if (!stopped) worker.schedule(action, delay, unit);
  }

  public void executeBlocking(Runnable action) {
    host.executeBlocking(action);
  }

  public void executeBlockingLater(Runnable action, long delay, TimeUnit unit) {
    executeAsyncLater(() -> executeBlocking(action), delay, unit);
  }

  PlatformHost host() {
    return host;
  }

  SdkSession session() {
    return sdk;
  }

  long nowMillis() {
    return clock.millis();
  }

  boolean isCurrent(SdkSession connection) {
    return connection != null && !stopped && setup && sdk == connection && !connection.isClosed();
  }

  boolean isReportingEnabled() {
    return isSetup()
        && config != null
        && config.isAutoReportEnabled()
        && getAccount() != null
        && getAccount().isLogEvents();
  }

  void tickSafely() {
    try {
      if (stopped || !setup) return;
      long now = nowMillis();
      delivery.tick(now);
      if (now >= nextRefresh && !refreshing) refreshListings();
      events.tick(now);
    } catch (Exception error) {
      error("Tebex background task failed.", error);
    }
  }

  void setConfigForTesting(ServerPlatformConfig value) {
    config = value;
  }

  void setSetupForTesting(boolean value) {
    setup = value;
  }

  void setStoreInformationForTesting(ServerInformation information) {
    storeInformation = information;
  }

  void setStoreCategoriesForTesting(List<Category> categories) {
    storeCategories = categories;
  }

  java.util.Map<Object, Integer> queuedPlayersForTesting() {
    return delivery.queuedPlayerSnapshot();
  }

  List<PluginEvent> pluginEventsForTesting() {
    return isReportingEnabled() ? events.pluginSnapshot() : Collections.emptyList();
  }

  private <T> CompletableFuture<T> track(CompletableFuture<T> future) {
    outstanding.add(future);
    future.whenComplete((value, error) -> outstanding.remove(future));
    if (stopped) future.completeExceptionally(new CancellationException("Plugin stopped"));
    return future;
  }

  private ServerInformation validateServerInformation(ServerInformation information) {
    if (information == null
        || information.getAccount() == null
        || information.getServer() == null
        || information.getAccount().getCurrency() == null)
      throw new IllegalStateException("Incomplete Tebex server information");
    return information;
  }

  private CompletableFuture<ServerInformation> adopt(
      SdkSession candidate,
      ServerInformation information,
      String key,
      boolean persist,
      long requestGeneration) {
    CompletableFuture<ServerInformation> result = track(new CompletableFuture<>());
    executeBlocking(
        () -> {
          synchronized (TebexRuntime.this) {
            try {
              if (stopped || generation != requestGeneration) {
                candidate.close();
                result.completeExceptionally(new CancellationException("Connection superseded"));
                return;
              }
              if (persist) persistSecret(key);
              if (isCurrent(sdk) && sdk.getSecretKey().equals(candidate.getSecretKey())) {
                candidate.close();
                storeInformation = information;
                nextRefresh = 0;
                result.complete(information);
                return;
              }
              if (sdk != null) sdk.close();
              sdk = candidate;
              storeInformation = information;
              storeCategories = Collections.emptyList();
              players.clear();
              setup = true;
              nextRefresh = 0;
              refreshing = false;
              delivery.connected(candidate);
              events.connected(nowMillis());
              info(
                  "Connected to "
                      + information.getAccount().getName()
                      + " as "
                      + information.getServer().getName());
              checkForUpdate(candidate);
              result.complete(information);
            } catch (Exception failure) {
              candidate.close();
              result.completeExceptionally(failure);
            }
          }
        });
    return result;
  }

  private void checkForUpdate(SdkSession connection) {
    if (config == null || !config.isCheckForUpdates()) return;

    connection
        .request(
            (api, ignoredSecret) ->
                api.checkForUpdate(getType().name().toLowerCase(Locale.ROOT), getPluginVersion()))
        .thenAccept(
            update -> {
              if (update != null && isCurrent(connection)) {
                info(
                    "A new update for Tebex ("
                        + update.getVersion()
                        + ") is available: https://creator.tebex.io/plugins");
              }
            })
        .exceptionally(ignored -> null);
  }

  private void persistSecret(String key) {
    String oldKey = config.getSecretKey();
    try {
      config.setSecretKey(key);
      saveConfig(config);
    } catch (RuntimeException failure) {
      config.setSecretKey(oldKey);
      throw failure;
    }
  }

  private void syncSdkDebugMode() {
    TXE.DEBUG_MODE = config != null && config.isVerbose();
  }

  private static Throwable rootCause(Throwable error) {
    Throwable current = error;
    while (current != null && current.getCause() != null && current.getCause() != current) {
      current = current.getCause();
    }
    return current;
  }

  private static String throwableSummary(Throwable error) {
    if (error == null) return "";
    String message = error.getMessage();
    return "["
        + error.getClass().getSimpleName()
        + (message == null || message.trim().isEmpty() ? "" : ": " + message)
        + "]";
  }

  private static <T> CompletableFuture<T> failed(String message) {
    CompletableFuture<T> rejected = new CompletableFuture<>();
    rejected.completeExceptionally(new IllegalStateException(message));
    return rejected;
  }

  private static <T> CompletableFuture<T> cancelled(String message) {
    CompletableFuture<T> rejected = new CompletableFuture<>();
    rejected.completeExceptionally(new CancellationException(message));
    return rejected;
  }
}
