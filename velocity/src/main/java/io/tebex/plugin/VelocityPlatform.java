package io.tebex.plugin;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import io.tebex.minecraft.platform.PlatformHost;
import io.tebex.minecraft.platform.PlatformTelemetry;
import io.tebex.minecraft.platform.PlatformType;
import io.tebex.minecraft.runtime.TebexRuntime;
import io.tebex.minecraft.util.CommandResult;
import java.io.File;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

/** Velocity operations consumed by the platform-neutral runtime. */
public final class VelocityPlatform implements PlatformHost {
  private final TebexVelocityPlugin plugin;
  private final ProxyServer proxy;
  private final Logger logger;
  private final Path dataDirectory;
  private final TebexRuntime runtime;

  VelocityPlatform(
      TebexVelocityPlugin plugin, ProxyServer proxy, Logger logger, Path dataDirectory) {
    this.plugin = plugin;
    this.proxy = proxy;
    this.logger = logger;
    this.dataDirectory = dataDirectory;
    this.runtime = new TebexRuntime(this);
  }

  public ProxyServer proxy() {
    return proxy;
  }

  public TebexVelocityPlugin plugin() {
    return plugin;
  }

  public TebexRuntime runtime() {
    return runtime;
  }

  @Override
  public PlatformType getType() {
    return PlatformType.VELOCITY;
  }

  @Override
  public File getRunningDirectory() {
    return dataDirectory.toFile();
  }

  @Override
  public boolean isOnlineMode() {
    return proxy.getConfiguration().isOnlineMode();
  }

  @Override
  public CommandResult dispatchCommand(String command) {
    throw new UnsupportedOperationException("Velocity dispatch is asynchronous");
  }

  @Override
  public CompletableFuture<CommandResult> dispatchCommandAsync(
      String command, Supplier<Boolean> active) {
    if (!active.get()) return CompletableFuture.completedFuture(null);
    return proxy
        .getCommandManager()
        .executeAsync(proxy.getConsoleCommandSource(), command)
        .handle(
            (success, error) ->
                error == null
                    ? CommandResult.from(Boolean.TRUE.equals(success))
                    : CommandResult.from(false)
                        .withException(error)
                        .withMessage(error.getMessage()));
  }

  @Override
  public void executeBlocking(Runnable action) {
    proxy.getScheduler().buildTask(plugin, action).schedule();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getPlayer(Object id) {
    if (id instanceof UUID) return (T) proxy.getPlayer((UUID) id).orElse(null);
    if (id instanceof String) return (T) proxy.getPlayer((String) id).orElse(null);
    return null;
  }

  @Override
  public int getFreeSlots(Object player) {
    return 0;
  }

  @Override
  public String getPluginVersion() {
    return Constants.VERSION;
  }

  @Override
  public void log(Level level, String message) {
    logger.atLevel(convertLevel(level)).log(message);
  }

  @Override
  public PlatformTelemetry getTelemetry() {
    return PlatformTelemetry.current(
        getPluginVersion(),
        proxy.getVersion().getName(),
        proxy.getVersion().getVersion(),
        proxy.getConfiguration().isOnlineMode());
  }

  @Override
  public void sendPlayerMessage(String playerName, String message) {
    Player player = getPlayer(playerName);
    if (player != null) player.sendMessage(Component.text(message));
  }

  @Override
  public UUID getPlayerUniqueId(String playerName) {
    Player player = getPlayer(playerName);
    return player == null ? null : player.getUniqueId();
  }

  @Override
  public boolean hasPermission(String username, String permission) {
    Player player = getPlayer(username);
    return player != null && player.hasPermission(permission);
  }

  private static org.slf4j.event.Level convertLevel(Level level) {
    if (level == Level.SEVERE) return org.slf4j.event.Level.ERROR;
    if (level == Level.WARNING) return org.slf4j.event.Level.WARN;
    if (level == Level.INFO) return org.slf4j.event.Level.INFO;
    if (level == Level.CONFIG || level == Level.FINE) return org.slf4j.event.Level.DEBUG;
    return org.slf4j.event.Level.TRACE;
  }
}
