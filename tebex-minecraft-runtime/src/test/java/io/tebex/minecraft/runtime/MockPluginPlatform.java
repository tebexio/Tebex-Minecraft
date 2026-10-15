package io.tebex.minecraft.runtime;

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
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;

/** Test host proving that the production runtime is composed rather than inherited. */
public class MockPluginPlatform implements PlatformHost {
  protected ServerPlatformConfig config;
  private final TebexRuntime runtime;

  public MockPluginPlatform() {
    this(SdkSession::new, System::currentTimeMillis);
  }

  MockPluginPlatform(SessionFactory sessions, RuntimeClock clock) {
    runtime = new TebexRuntime(this, sessions, clock);
    config = new ServerPlatformConfig(1);
    config.setVerbose(true);
    runtime.setConfigForTesting(config);
  }

  public TebexRuntime getRuntime() {
    return runtime;
  }

  public CompletableFuture<ServerInformation> connect(String key, boolean persist) {
    return runtime.connect(key, persist);
  }

  public SdkSession getSession() {
    return runtime.getSession();
  }

  public boolean isSetup() {
    return runtime.isSetup();
  }

  public void setSetup(boolean setup) {
    runtime.setSetupForTesting(setup);
  }

  public void shutdown() {
    runtime.shutdown();
  }

  public <T> CompletableFuture<T> callPlayer(Object player, Supplier<T> action) {
    return runtime.callPlayer(player, action);
  }

  public CompletableFuture<String[]> checkCommandQueue() {
    return runtime.checkCommandQueue();
  }

  public void tickSafely() {
    runtime.tickSafely();
  }

  public Map<Object, Integer> getQueuedPlayers() {
    return runtime.queuedPlayersForTesting();
  }

  public void createPluginEvent(PluginEvent.Level level, String message) {
    runtime.createPluginEvent(level, message);
  }

  public List<PluginEvent> snapshotPluginEvents() {
    return runtime.pluginEventsForTesting();
  }

  public void setStoreInfo(ServerInformation information) {
    runtime.setStoreInformationForTesting(information);
  }

  public void setStoreCategories(List<Category> categories) {
    runtime.setStoreCategoriesForTesting(categories);
  }

  public List<Category> getStoreCategories() {
    return runtime.getStoreCategories();
  }

  public TebexCommands getCommands() {
    return runtime.getCommands();
  }

  public String buyAccessError(String username) {
    return runtime.buyAccessError(username);
  }

  public String resolveCommandPlayerId(QueuedPlayer player) {
    return runtime.resolveCommandPlayerId(player);
  }

  public void sendCheckoutLink(String playerName, String checkoutUrl) {
    runtime.sendCheckoutLink(playerName, checkoutUrl);
  }

  public ServerPlatformConfig getConfig() {
    return runtime.getConfig();
  }

  public void setConfig(ServerPlatformConfig value) {
    config = value;
    runtime.setConfigForTesting(value);
  }

  @Override
  public String getPluginVersion() {
    return "1.0.0";
  }

  @Override
  public PlatformType getType() {
    return PlatformType.BUKKIT;
  }

  @Override
  public File getRunningDirectory() {
    return null;
  }

  @Override
  public void log(Level level, String message) {}

  @Override
  public CommandResult dispatchCommand(String command) {
    return null;
  }

  @Override
  public boolean isOnlineMode() {
    return false;
  }

  @Override
  public <T> T getPlayer(Object id) {
    return null;
  }

  @Override
  public int getFreeSlots(Object player) {
    return 0;
  }

  @Override
  public PlatformTelemetry getTelemetry() {
    return new PlatformTelemetry("1.0.0", "mock", "1.0.0", "18", "x64", true);
  }

  @Override
  public void sendPlayerMessage(String playerName, String message) {}

  @Override
  public boolean hasPermission(String username, String permission) {
    return false;
  }

  @Override
  public void executeBlocking(Runnable action) {
    action.run();
  }
}
