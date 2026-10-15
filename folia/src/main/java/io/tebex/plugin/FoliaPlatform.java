package io.tebex.plugin;

import io.tebex.minecraft.platform.PlatformHost;
import io.tebex.minecraft.platform.PlatformTelemetry;
import io.tebex.minecraft.platform.PlatformType;
import io.tebex.minecraft.platform.config.ServerPlatformConfig;
import io.tebex.minecraft.runtime.TebexRuntime;
import io.tebex.minecraft.util.CommandResult;
import java.io.File;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandException;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Folia operations consumed by the platform-neutral runtime. */
public final class FoliaPlatform implements PlatformHost {
  private final TebexFoliaPlugin plugin;
  private final TebexRuntime runtime;

  FoliaPlatform(TebexFoliaPlugin plugin) {
    this.plugin = plugin;
    this.runtime = new TebexRuntime(this);
  }

  public TebexRuntime runtime() {
    return runtime;
  }

  @Override
  public int getFreeSlots(Object playerId) {
    Player player = getPlayer(playerId);
    if (player == null) return -1;
    ItemStack[] inventory = Arrays.copyOfRange(player.getInventory().getContents(), 0, 36);
    return (int)
        Arrays.stream(inventory)
            .filter(item -> item == null || item.getType() == Material.AIR)
            .count();
  }

  @Override
  public File getRunningDirectory() {
    return plugin.getDataFolder();
  }

  @Override
  public boolean isOnlineMode() {
    ServerPlatformConfig config = runtime.getConfig();
    return Bukkit.getServer().getOnlineMode() || (config != null && config.isProxyMode());
  }

  @Override
  public CommandResult dispatchCommand(String command) {
    if (!plugin.isEnabled()) return CommandResult.from(false).withMessage("Store is not enabled.");
    try {
      return CommandResult.from(
          Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command));
    } catch (CommandException error) {
      return CommandResult.from(false).withMessage(error.getMessage()).withException(error);
    }
  }

  @Override
  public void executeBlocking(Runnable action) {
    if (!runtime.isStopped() && plugin.isEnabled())
      plugin.getServer().getGlobalRegionScheduler().execute(plugin, action);
  }

  @Override
  public void executeForPlayer(Object id, Runnable action, Runnable retired) {
    if (runtime.isStopped() || !plugin.isEnabled()) {
      retired.run();
      return;
    }
    Player player = getPlayer(id);
    if (player == null) {
      retired.run();
      return;
    }
    player.getScheduler().execute(plugin, action, retired, 1L);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getPlayer(Object id) {
    if (id instanceof UUID) return (T) Bukkit.getServer().getPlayer((UUID) id);
    if (id instanceof String) return (T) Bukkit.getServer().getPlayerExact((String) id);
    return null;
  }

  @Override
  public boolean isPlayerOnline(Object playerId) {
    Player player = getPlayer(playerId);
    return player != null && player.isOnline();
  }

  @Override
  public void log(Level level, String message) {
    plugin.getLogger().log(level, message);
  }

  @Override
  public String getPluginVersion() {
    return plugin.getDescription().getVersion();
  }

  @Override
  public PlatformTelemetry getTelemetry() {
    return PlatformTelemetry.current(
        getPluginVersion(),
        plugin.getServer().getName(),
        plugin.getServer().getVersion(),
        Bukkit.getOnlineMode());
  }

  @Override
  public void sendPlayerMessage(String playerName, String message) {
    Player player = getPlayer(playerName);
    if (player != null) player.sendMessage(message);
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

  @Override
  public PlatformType getType() {
    return PlatformType.FOLIA;
  }
}
