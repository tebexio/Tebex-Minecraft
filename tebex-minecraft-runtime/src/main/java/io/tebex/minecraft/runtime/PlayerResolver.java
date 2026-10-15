package io.tebex.minecraft.runtime;

import io.tebex.minecraft.platform.PlatformHost;
import io.tebex.minecraft.util.PlayerIdentity;
import io.tebex.minecraft.util.UUIDUtil;
import io.tebex.model.QueuedPlayer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import org.geysermc.floodgate.util.LinkedPlayer;

/** Resolves Java/Bedrock identities and chooses the checkout-link presentation. */
final class PlayerResolver {
  private final PlatformHost host;
  private final Consumer<String> debug;
  private final BiConsumer<String, String> warning;
  private final Map<String, UUID> floodgateIds = new ConcurrentHashMap<>();
  private final AtomicBoolean floodgateWarningLogged = new AtomicBoolean();

  PlayerResolver(PlatformHost host, Consumer<String> debug, BiConsumer<String, String> warning) {
    this.host = host;
    this.debug = debug;
    this.warning = warning;
  }

  Object playerId(String name, UUID uuid, boolean onlineMode, boolean geyserStore) {
    return onlineMode && !geyserStore && uuid != null && !uuid.equals(UUIDUtil.EMPTY_UUID)
        ? uuid
        : name == null ? "" : name;
  }

  String commandPlayerId(QueuedPlayer player, boolean geyserStore) {
    if (player == null) return "";
    if (PlayerIdentity.hasUuid(player)) return PlayerIdentity.defaultIdentifier(player);
    if (PlayerIdentity.hasXuid(player)) {
      UUID id =
          floodgateIds.computeIfAbsent(
              player.getXuid(), ignored -> resolveFloodgateUniqueId(player, geyserStore));
      if (id != null) return id.toString();
    }
    return PlayerIdentity.defaultIdentifier(player);
  }

  void sendCheckoutLink(String playerName, String url) {
    if (isOnlineFloodgatePlayer(playerName))
      host.sendPlayerMessage(
          playerName, "Checkout started! Type this link into your browser: " + url);
    else host.sendJavaCheckoutLink(playerName, url);
  }

  void clear() {
    floodgateIds.clear();
  }

  private UUID resolveFloodgateUniqueId(QueuedPlayer player, boolean geyserStore) {
    if (!geyserStore) return null;

    Long xuid = PlayerIdentity.xuid(player);
    if (xuid == null) {
      debug.accept(
          "Unable to parse Bedrock XUID '"
              + player.getXuid()
              + "' for player '"
              + player.getName()
              + "'.");
      return null;
    }

    try {
      FloodgateApi api = FloodgateApi.getInstance();
      UUID bedrockId = api.createJavaPlayerId(xuid);
      if (bedrockId == null || UUIDUtil.EMPTY_UUID.equals(bedrockId)) return null;

      FloodgatePlayer onlinePlayer = api.getPlayer(bedrockId);
      if (onlinePlayer != null && onlinePlayer.getCorrectUniqueId() != null)
        return onlinePlayer.getCorrectUniqueId();

      if (api.getPlayerLink() != null) {
        try {
          LinkedPlayer linkedPlayer =
              api.getPlayerLink().getLinkedPlayer(bedrockId).get(2, TimeUnit.SECONDS);
          if (linkedPlayer != null && linkedPlayer.getJavaUniqueId() != null)
            return linkedPlayer.getJavaUniqueId();
        } catch (InterruptedException error) {
          Thread.currentThread().interrupt();
          debug.accept(
              "Interrupted while resolving Floodgate UUID for player '" + player.getName() + "'.");
        } catch (ExecutionException | TimeoutException error) {
          debug.accept(
              "Failed to resolve Floodgate link data for player '"
                  + player.getName()
                  + "': "
                  + error.getMessage());
        }
      }
      return bedrockId;
    } catch (IllegalStateException | NoClassDefFoundError error) {
      warnMissingFloodgateApi();
      return null;
    }
  }

  private boolean isOnlineFloodgatePlayer(String playerName) {
    UUID uniqueId = host.getPlayerUniqueId(playerName);
    if (uniqueId == null) return false;
    try {
      return FloodgateApi.getInstance().isFloodgatePlayer(uniqueId);
    } catch (IllegalStateException | NoClassDefFoundError error) {
      return false;
    }
  }

  private void warnMissingFloodgateApi() {
    if (!floodgateWarningLogged.compareAndSet(false, true)) return;
    warning.accept(
        "Received a Bedrock XUID for command placeholder resolution, but the Floodgate API is unavailable.",
        "Install Floodgate on the same server or proxy running Tebex so {id}/{uuid} placeholders can be resolved for Bedrock players.");
  }
}
