package io.tebex.minecraft.platform;

import io.tebex.minecraft.util.CommandResult;
import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Native operations required by the platform-neutral Tebex runtime.
 *
 * <p>Implementations live in each distributable variant. This interface deliberately contains no
 * Tebex connection, queue, command, configuration, or catalogue lifecycle.
 */
public interface PlatformHost {
  String getPluginVersion();

  PlatformType getType();

  File getRunningDirectory();

  void log(Level level, String message);

  CommandResult dispatchCommand(String command);

  /**
   * Return a native asynchronous dispatch future, or {@code null} to use server-thread dispatch.
   */
  default CompletableFuture<CommandResult> dispatchCommandAsync(
      String command, Supplier<Boolean> active) {
    return null;
  }

  boolean isOnlineMode();

  <T> T getPlayer(Object uuidOrUsername);

  default boolean isPlayerOnline(Object player) {
    return getPlayer(player) != null;
  }

  default UUID getPlayerUniqueId(String playerName) {
    return null;
  }

  int getFreeSlots(Object player);

  PlatformTelemetry getTelemetry();

  void sendPlayerMessage(String playerName, String message);

  default void sendJavaCheckoutLink(String playerName, String checkoutUrl) {
    sendPlayerMessage(playerName, "Checkout started! Complete payment here: " + checkoutUrl);
  }

  boolean hasPermission(String username, String permission);

  void executeBlocking(Runnable action);

  default void executeForPlayer(Object player, Runnable action, Runnable retired) {
    executeBlocking(action);
  }
}
