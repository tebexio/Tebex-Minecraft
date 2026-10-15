package io.tebex.plugin;

import io.tebex.minecraft.platform.PlatformHost;
import io.tebex.minecraft.platform.PlatformTelemetry;
import io.tebex.minecraft.platform.PlatformType;
import io.tebex.minecraft.runtime.TebexRuntime;
import io.tebex.minecraft.util.CommandResult;
import java.io.File;
import java.util.UUID;
import java.util.logging.Level;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/** BungeeCord operations consumed by the platform-neutral runtime. */
public final class BungeePlatform implements PlatformHost {
  private final TebexBungeePlugin plugin;
  private final TebexRuntime runtime;

  public BungeePlatform(TebexBungeePlugin plugin) {
    this.plugin = plugin;
    this.runtime = new TebexRuntime(this);
  }

  public TebexRuntime runtime() {
    return runtime;
  }

  @Override
  public PlatformType getType() {
    return PlatformType.BUNGEECORD;
  }

  @Override
  public File getRunningDirectory() {
    return plugin.getDataFolder();
  }

  @Override
  public boolean isOnlineMode() {
    return false;
  }

  @Override
  public CommandResult dispatchCommand(String command) {
    return CommandResult.from(
        plugin
            .getProxy()
            .getPluginManager()
            .dispatchCommand(plugin.getProxy().getConsole(), command));
  }

  @Override
  public void executeBlocking(Runnable action) {
    plugin.getProxy().getScheduler().runAsync(plugin, action);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getPlayer(Object id) {
    if (id instanceof UUID) return (T) plugin.getProxy().getPlayer((UUID) id);
    if (id instanceof String) return (T) plugin.getProxy().getPlayer((String) id);
    return null;
  }

  @Override
  public int getFreeSlots(Object player) {
    return 0;
  }

  @Override
  public String getPluginVersion() {
    return plugin.getDescription().getVersion();
  }

  @Override
  public void log(Level level, String message) {
    plugin.getLogger().log(level, message);
  }

  @Override
  public PlatformTelemetry getTelemetry() {
    return PlatformTelemetry.current(
        getPluginVersion(),
        plugin.getProxy().getName(),
        plugin.getProxy().getVersion(),
        plugin.getProxy().getConfig().isOnlineMode());
  }

  @Override
  public void sendPlayerMessage(String playerName, String message) {
    ProxiedPlayer player = getPlayer(playerName);
    if (player != null) player.sendMessage(TextComponent.fromLegacyText(message));
  }

  @Override
  public UUID getPlayerUniqueId(String playerName) {
    ProxiedPlayer player = getPlayer(playerName);
    return player == null ? null : player.getUniqueId();
  }

  @Override
  public boolean hasPermission(String username, String permission) {
    ProxiedPlayer player = getPlayer(username);
    return player != null && player.hasPermission(permission);
  }
}
