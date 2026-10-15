package io.tebex.minecraft.runtime;

import io.tebex.minecraft.util.CommandResult;
import io.tebex.minecraft.util.PlayerIdentity;
import io.tebex.model.DuePlayersResponse;
import io.tebex.model.QueuedCommand;
import io.tebex.model.QueuedPlayer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/** Polls commands, evaluates delivery conditions, dispatches, and acknowledges successful work. */
final class CommandDelivery {
  private final TebexRuntime runtime;
  private final Map<Object, Integer> queuedPlayers = new ConcurrentHashMap<>();
  private volatile DeliveryQueue queue;
  private volatile long nextCheck;
  private volatile long nextAcknowledgement;
  private volatile boolean acknowledging;
  private CompletableFuture<String[]> checking;

  CommandDelivery(TebexRuntime runtime) {
    this.runtime = runtime;
  }

  synchronized void connected(SdkSession connection) {
    if (queue != null) queue.close();
    queue = new DeliveryQueue(runtime::nowMillis, command -> deliver(connection, command));
    queuedPlayers.clear();
    nextCheck = 0;
    nextAcknowledgement = 0;
    acknowledging = false;
    checking = null;
  }

  void tick(long now) {
    DeliveryQueue activeQueue = queue;
    if (activeQueue == null) return;
    activeQueue.pump();
    if (now >= nextCheck) check();
    if (now >= nextAcknowledgement && !acknowledging) acknowledge(activeQueue);
  }

  synchronized CompletableFuture<String[]> check() {
    if (!runtime.isSetup())
      return CompletableFuture.completedFuture(new String[] {"Tebex is not connected."});
    if (checking != null && !checking.isDone()) return checking;

    final SdkSession connection = runtime.session();
    final DeliveryQueue activeQueue = queue;
    nextCheck = runtime.nowMillis() + 120_000L;
    checking =
        connection
            .request((api, secret) -> api.getDuePlayers(secret))
            .thenCompose(due -> fetchCommands(connection, activeQueue, due))
            .handle(
                (ignored, error) -> {
                  if (error != null && runtime.isCurrent(connection)) {
                    String message = String.valueOf(error.getMessage());
                    nextCheck =
                        runtime.nowMillis()
                            + (message.contains("429")
                                ? 300_000L
                                : message.contains("403") ? 1_800_000L : 60_000L);
                    runtime.warning(
                        "Queue check failed.", "It will be retried automatically.", error);
                    return new String[] {"Queue check failed; retry scheduled."};
                  }
                  return new String[] {"Queue check completed."};
                });
    return checking;
  }

  boolean hasQueuedPlayer(Object playerId) {
    return queuedPlayers.containsKey(playerId);
  }

  Map<Object, Integer> queuedPlayerSnapshot() {
    return Collections.unmodifiableMap(new java.util.HashMap<>(queuedPlayers));
  }

  synchronized void close() {
    if (queue != null) queue.close();
    queue = null;
    queuedPlayers.clear();
    checking = null;
  }

  private CompletableFuture<Void> fetchCommands(
      SdkSession connection, DeliveryQueue activeQueue, DuePlayersResponse due) {
    if (!runtime.isCurrent(connection)) return CompletableFuture.completedFuture(null);
    nextCheck = runtime.nowMillis() + Math.max(1, due.getNextCheck()) * 1000L;
    List<CompletableFuture<?>> requests = new ArrayList<>();
    if (due.isExecuteOffline())
      requests.add(
          connection
              .request((api, secret) -> api.getOfflineCommands(secret))
              .thenAccept(
                  response -> {
                    if (runtime.isCurrent(connection))
                      response.getCommands().forEach(activeQueue::offer);
                  }));

    queuedPlayers.clear();
    for (QueuedPlayer player : due.getPlayers()) {
      Object playerId = runtime.playerId(player.getName(), PlayerIdentity.uuid(player.getUuid()));
      queuedPlayers.put(playerId, player.getId());
      requests.add(
          runtime
              .callPlayer(playerId, () -> runtime.host().isPlayerOnline(playerId))
              .handle((online, error) -> error == null && Boolean.TRUE.equals(online))
              .thenCompose(
                  online -> {
                    if (!online || !runtime.isCurrent(connection))
                      return CompletableFuture.completedFuture(null);
                    return connection
                        .request((api, secret) -> api.getOnlineCommands(secret, player))
                        .thenAccept(
                            commands -> {
                              if (runtime.isCurrent(connection))
                                commands.forEach(activeQueue::offer);
                            });
                  }));
    }
    return CompletableFuture.allOf(requests.toArray(new CompletableFuture<?>[0]));
  }

  private CompletableFuture<Boolean> deliver(SdkSession connection, QueuedCommand command) {
    if (!runtime.isCurrent(connection)) return CompletableFuture.completedFuture(false);
    QueuedPlayer player = command.getPlayer();
    String playerName = player == null ? "unknown" : player.getName();
    Object id =
        player == null
            ? ""
            : runtime.playerId(player.getName(), PlayerIdentity.uuid(player.getUuid()));
    String raw = command.getCommand();
    String parsed =
        raw == null || raw.trim().isEmpty() ? raw : PlayerIdentity.command(command, runtime);
    if (parsed == null || parsed.trim().isEmpty()) {
      StringBuilder message =
          new StringBuilder(
              String.format(
                  "Command #%d for player '%s' is blank and cannot be executed. Check the commands configured on this package in your webstore.",
                  command.getId(), playerName));
      if (command.getPackageId() != 0)
        message.append(" Package: ").append(command.getPackageId()).append('.');
      if (command.getPaymentId() != 0)
        message
            .append(" Payment: https://creator.tebex.io/payments/")
            .append(command.getPaymentId());
      runtime.error(message.toString());
      return CompletableFuture.completedFuture(false);
    }
    CompletableFuture<Boolean> eligible =
        command.isOnline()
            ? runtime.callPlayer(
                id,
                () ->
                    player != null
                        && (!runtime.host().isOnlineMode()
                            || runtime.isGeyser()
                            || PlayerIdentity.hasUuid(player))
                        && runtime.host().isPlayerOnline(id)
                        && runtime.host().getFreeSlots(id) >= command.getRequiredSlots())
            : CompletableFuture.completedFuture(true);
    return eligible
        .thenCompose(
            allowed -> {
              if (!allowed || !runtime.isCurrent(connection))
                return CompletableFuture.completedFuture(false);
              return runtime
                  .dispatchAsync(parsed, () -> runtime.isCurrent(connection))
                  .thenApply(result -> dispatched(result));
            })
        .exceptionally(
            error -> {
              if (runtime.isCurrent(connection))
                runtime.warning(
                    "Delivery deferred.", "It will be retried on a later queue check.", error);
              return false;
            });
  }

  private boolean dispatched(CommandResult result) {
    if (result == null) return false;
    if (!result.isSuccess())
      runtime.warning(
          "Command failed: " + result.getMessage(), "Check the command syntax in the store.");
    return true;
  }

  private void acknowledge(DeliveryQueue activeQueue) {
    List<Integer> ids = activeQueue.acknowledgements();
    nextAcknowledgement = runtime.nowMillis() + 5000L;
    if (ids.isEmpty()) return;
    acknowledging = true;
    SdkSession connection = runtime.session();
    connection
        .request((api, secret) -> api.deleteCommands(secret, ids))
        .whenComplete(
            (success, error) -> {
              if (runtime.isCurrent(connection) && Boolean.TRUE.equals(success))
                activeQueue.acknowledged(ids);
              if (runtime.isCurrent(connection)) acknowledging = false;
            });
  }
}
