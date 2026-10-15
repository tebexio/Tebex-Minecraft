package io.tebex.plugin;

import io.tebex.minecraft.platform.PlatformHost;
import io.tebex.minecraft.platform.PlatformTelemetry;
import io.tebex.minecraft.platform.PlatformType;
import io.tebex.minecraft.platform.config.ServerPlatformConfig;
import io.tebex.minecraft.runtime.TebexRuntime;
import io.tebex.minecraft.util.BuildInfo;
import io.tebex.minecraft.util.CommandResult;
import io.tebex.plugin.compat.ForgePermissionNodes;
import io.tebex.plugin.compat.NativeVersion;
import java.io.File;
import java.util.UUID;
import java.util.logging.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Variant-local server binding, compiled and remapped against this target's Minecraft API. */
public final class ForgePlatform implements PlatformHost {
  private static final Logger LOGGER = LogManager.getLogger("tebex");
  private static final File MOD_PATH = new File("./mods/tebex");
  private final TebexRuntime runtime;
  private MinecraftServer server;

  ForgePlatform() {
    this.runtime = new TebexRuntime(this);
  }

  public final MinecraftServer getServer() {
    return server;
  }

  final void attachServer(MinecraftServer server) {
    this.server = server;
  }

  @Override
  public final PlatformType getType() {
    return PlatformType.FORGE;
  }

  @Override
  public final File getRunningDirectory() {
    return MOD_PATH;
  }

  @Override
  public final boolean isOnlineMode() {
    ServerPlatformConfig config = runtime.getConfig();
    return (config != null && config.isProxyMode()) || server.usesAuthentication();
  }

  @Override
  public final CommandResult dispatchCommand(String command) {
    NativeVersion.dispatch(server, command);
    return CommandResult.from(true); // Native dispatch reports exceptions, not a success result.
  }

  @Override
  @SuppressWarnings("unchecked")
  public final <T> T getPlayer(Object id) {
    if (server == null) return null;
    if (id instanceof UUID) return (T) server.getPlayerList().getPlayer((UUID) id);
    if (id instanceof String) return (T) server.getPlayerList().getPlayerByName((String) id);
    return null;
  }

  @Override
  public final UUID getPlayerUniqueId(String name) {
    ServerPlayer player = getPlayer(name);
    return player == null ? null : player.getUUID();
  }

  @Override
  public final int getFreeSlots(Object id) {
    ServerPlayer player = getPlayer(id);
    if (player == null) return -1;
    int free = 0;
    for (ItemStack stack : NativeVersion.inventory(player))
      if (stack == null || stack.isEmpty()) free++;
    return free;
  }

  @Override
  public final void log(Level level, String message) {
    if (level == Level.SEVERE) LOGGER.error(message);
    else if (level == Level.WARNING) LOGGER.warn(message);
    else LOGGER.info(message);
  }

  @Override
  public final PlatformTelemetry getTelemetry() {
    return PlatformTelemetry.current(
        getPluginVersion(), "FORGE", server.getServerVersion(), server.usesAuthentication());
  }

  @Override
  public final String getPluginVersion() {
    return BuildInfo.version();
  }

  @Override
  public final void sendJavaCheckoutLink(String name, String url) {
    ServerPlayer player = getPlayer(name);
    if (player != null) player.sendSystemMessage(NativeVersion.checkoutLink(url));
  }

  @Override
  public final void sendPlayerMessage(String name, String message) {
    ServerPlayer player = getPlayer(name);
    if (player != null) player.sendSystemMessage(Component.nullToEmpty(message));
  }

  @Override
  public final void executeBlocking(Runnable action) {
    if (!runtime.isStopped() && server != null) server.execute(action);
  }

  final TebexRuntime runtime() {
    return runtime;
  }

  @Override
  public boolean hasPermission(String name, String permission) {
    ServerPlayer player = getPlayer(name);
    return player != null && ForgePermissionNodes.hasPermission(player, permission);
  }
}
