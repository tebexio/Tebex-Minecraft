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

/** Bukkit operations consumed by the platform-neutral runtime. */
public final class BukkitPlatform implements PlatformHost {
  private final TebexBukkitPlugin plugin;
  private final TebexRuntime runtime;

  BukkitPlatform(TebexBukkitPlugin plugin) {
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
    if (!plugin.isEnabled() || runtime.isStopped()) return;
    if (Bukkit.isPrimaryThread()) action.run();
    else Bukkit.getServer().getScheduler().runTask(plugin, action);
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
    return PlatformType.BUKKIT;
  }

  @Override
  public void executeForPlayer(Object player, Runnable action, Runnable retired) {
    // Plain Bukkit work belongs on the main server thread. Calling back into
    // TebexRuntime here recurses forever because TebexRuntime delegates right
    // back to its PlatformHost.
    executeBlocking(action);
  }
}
