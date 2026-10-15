package io.tebex.minecraft.runtime;

import io.tebex.model.PluginEvent;
import io.tebex.model.ServerEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Buffers join and plugin events and reports bounded batches through the active SDK session. */
final class EventReporter {
  private static final int CAPACITY = 1000;
  private static final int BATCH_SIZE = 750;
  private static final long JOIN_INTERVAL_MILLIS = 60_000L;
  private static final long LOG_INTERVAL_MILLIS = 600_000L;

  private final TebexRuntime runtime;
  private final List<ServerEvent> joins = Collections.synchronizedList(new ArrayList<>());
  private final List<PluginEvent> logs = Collections.synchronizedList(new ArrayList<>());
  private volatile long nextJoins;
  private volatile long nextLogs;
  private volatile boolean sendingJoins;
  private volatile boolean sendingLogs;

  EventReporter(TebexRuntime runtime) {
    this.runtime = runtime;
  }

  void connected(long now) {
    clear();
    sendingJoins = false;
    sendingLogs = false;
    nextJoins = now + JOIN_INTERVAL_MILLIS;
    nextLogs = now + LOG_INTERVAL_MILLIS;
  }

  void tick(long now) {
    if (now >= nextJoins && !sendingJoins) flushJoins(now);
    if (now >= nextLogs && !sendingLogs) flushLogs(now);
  }

  void recordJoin(String uuid, String username, String ip) {
    synchronized (joins) {
      if (!runtime.isStopped() && joins.size() < CAPACITY)
        joins.add(new ServerEvent(uuid, username, ip, ServerEvent.Type.JOIN));
    }
  }

  void recordPlugin(PluginEvent event) {
    synchronized (logs) {
      if (logs.size() < CAPACITY) logs.add(event);
    }
  }

  List<PluginEvent> pluginSnapshot() {
    synchronized (logs) {
      return new ArrayList<>(logs.subList(0, Math.min(BATCH_SIZE, logs.size())));
    }
  }

  List<ServerEvent> joinSnapshot() {
    synchronized (joins) {
      return new ArrayList<>(joins.subList(0, Math.min(BATCH_SIZE, joins.size())));
    }
  }

  void clear() {
    joins.clear();
    logs.clear();
  }

  private void flushJoins(long now) {
    nextJoins = now + JOIN_INTERVAL_MILLIS;
    List<ServerEvent> batch = joinSnapshot();
    if (batch.isEmpty()) return;
    sendingJoins = true;
    SdkSession connection = runtime.session();
    connection
        .request((api, secret) -> api.sendJoinEvents(secret, batch))
        .whenComplete(
            (sent, error) -> {
              if (runtime.isCurrent(connection) && Boolean.TRUE.equals(sent))
                joins.removeAll(batch);
              if (runtime.isCurrent(connection)) sendingJoins = false;
            });
  }

  private void flushLogs(long now) {
    nextLogs = now + LOG_INTERVAL_MILLIS;
    if (!runtime.isReportingEnabled()) {
      logs.clear();
      return;
    }
    List<PluginEvent> batch = pluginSnapshot();
    if (batch.isEmpty()) return;
    sendingLogs = true;
    SdkSession connection = runtime.session();
    connection
        .request((api, secret) -> api.sendPluginEvents(batch))
        .whenComplete(
            (sent, error) -> {
              if (runtime.isCurrent(connection) && Boolean.TRUE.equals(sent)) logs.removeAll(batch);
              if (runtime.isCurrent(connection)) sendingLogs = false;
            });
  }
}
