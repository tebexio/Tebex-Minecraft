package io.tebex.minecraft.runtime;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import io.tebex.http.PluginApi;
import io.tebex.minecraft.util.CommandResult;
import io.tebex.model.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;

class RuntimeTest {
  private static final Gson GSON = new Gson();
  private Host host;

  @BeforeEach
  void setup() {
    host = new Host();
  }

  @AfterEach
  void stop() {
    host.shutdown();
  }

  @Test
  void rejectedKeyLeavesWorkingConnectionAndCatalogueAlone() {
    host.connect("good", false).join();
    SdkSession previous = host.getSession();
    assertThrows(CompletionException.class, () -> host.connect("bad", false).join());
    assertSame(previous, host.getSession());
    assertTrue(host.isSetup());
    assertFalse(previous.isClosed());
  }

  @Test
  void availablePluginUpdateIsLoggedToTheConsoleAfterConnection() {
    host.config.setCheckForUpdates(true);
    host.api.pluginVersion =
        GSON.fromJson(
            "{\"version\":\"2.4.6\",\"released\":\"2026-08-24T18:22:09+0000\"}",
            PluginVersion.class);

    host.connect("good", false).join();

    assertEquals("bukkit", host.api.updatePlatform);
    assertEquals("1.0.0", host.api.runningVersion);
    assertTrue(
        host.logs.contains(
            "A new update for Tebex (2.4.6) is available: https://creator.tebex.io/plugins"));
  }

  @Test
  void disabledPluginUpdateCheckDoesNotReachTheSdk() {
    host.connect("good", false).join();

    org.mockito.Mockito.verify(host.api.client, org.mockito.Mockito.never())
        .checkForUpdate(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void delayedOnlineDeliveryRechecksDisconnectedPlayerAndInventory() {
    host.connect("good", false).join();
    host.api.commands = Arrays.asList(command(1, 5, true, 2));
    host.checkCommandQueue().join();
    host.online = false;
    host.clock.now = 5000;
    host.tickSafely();
    assertTrue(host.executed.isEmpty());
    assertTrue(host.api.deleted.isEmpty());
    host.online = true;
    host.slots = 1;
    host.checkCommandQueue().join();
    host.clock.now = 10000;
    host.tickSafely();
    assertTrue(host.executed.isEmpty());
    host.slots = 3;
    host.checkCommandQueue().join();
    host.clock.now = 15000;
    host.tickSafely();
    assertEquals(Arrays.asList("say Steve"), host.executed);
  }

  @Test
  void failedAcknowledgementsRetryWithoutRunningTheCommandAgain() {
    host.connect("good", false).join();
    host.api.commands = Arrays.asList(command(1, 0, true, 0));
    host.api.acknowledge = false;
    host.checkCommandQueue().join();
    host.tickSafely();
    host.checkCommandQueue().join();
    host.clock.now = 5000;
    host.tickSafely();
    assertEquals(1, host.executed.size());
    assertEquals(2, host.api.deleteAttempts);
    host.api.acknowledge = true;
    host.clock.now = 10000;
    host.tickSafely();
    assertEquals(Arrays.asList(1), host.api.deleted);
  }

  @Test
  void completedFailedDispatchIsReportedAndAcknowledged() {
    host.connect("good", false).join();
    host.failDispatch = true;
    host.api.commands = Arrays.asList(command(4, 0, true, 0));
    host.checkCommandQueue().join();
    host.tickSafely();
    assertEquals(Arrays.asList(4), host.api.deleted);
  }

  @Test
  void blankQueuedCommandsAreReportedAndLeftUnacknowledged() {
    host.connect("good", false).join();
    QueuedCommand blank = queuedCommand(21, "   ", 456, 123);
    QueuedCommand missing = queuedCommand(22, null, 0, 0);
    host.api.commands = Arrays.asList(blank, missing);

    host.checkCommandQueue().join();
    host.tickSafely();
    host.tickSafely();

    assertTrue(host.executed.isEmpty());
    assertTrue(host.api.deleted.isEmpty());
    assertTrue(
        host.logs.stream()
            .anyMatch(
                message ->
                    message.contains("Command #21")
                        && message.contains("Package: 123")
                        && message.contains("payments/456")));
    assertTrue(host.logs.stream().anyMatch(message -> message.contains("Command #22")));
  }

  @Test
  void pendingDeliveryIsDiscardedOnConnectionChange() {
    host.connect("good", false).join();
    host.api.commands = Arrays.asList(command(1, 10, true, 0));
    host.checkCommandQueue().join();
    host.api.commands = Collections.emptyList();
    host.connect("other", false).join();
    host.clock.now = 20000;
    host.tickSafely();
    assertTrue(host.executed.isEmpty());
  }

  @Test
  void shutdownCancelsHostWorkThatNeverReachedTheScheduler() {
    host.defer = true;
    CompletableFuture<Integer> result = host.callPlayer("Steve", () -> 7);
    host.shutdown();
    assertTrue(result.isCompletedExceptionally());
    host.deferred.forEach(Runnable::run);
    assertTrue(result.isCompletedExceptionally());
  }

  @Test
  void overlappingChecksShareOneRequest() {
    host.connect("good", false).join();
    host.api.pendingDue = new CompletableFuture<>();
    CompletableFuture<String[]> first = host.checkCommandQueue();
    assertSame(first, host.checkCommandQueue());
    assertFalse(first.isDone());
    host.api.pendingDue.complete(due());
    assertTrue(first.isDone());
  }

  @Test
  void lateAuthenticationCannotReplaceNewerConnection() {
    host.api.pendingAuth = new CompletableFuture<>();
    CompletableFuture<ServerInformation> first = host.connect("slow", false);
    host.connect("good", false).join();
    host.api.pendingAuth.complete(info());
    assertTrue(first.isCompletedExceptionally());
    assertEquals("good", host.getSession().getSecretKey());
  }

  @Test
  void sameKeyReloadRetainsUnacknowledgedDispatches() {
    host.connect("good", false).join();
    SdkSession original = host.getSession();
    host.api.commands = Arrays.asList(command(7, 0, true, 0));
    host.api.acknowledge = false;
    host.checkCommandQueue().join();
    host.tickSafely();
    host.connect("good", false).join();
    assertSame(original, host.getSession());
    host.checkCommandQueue().join();
    host.api.acknowledge = true;
    host.clock.now = 5000;
    host.tickSafely();
    assertEquals(1, host.executed.size());
    assertEquals(Arrays.asList(7), host.api.deleted);
  }

  @Test
  void malformedAuthenticationCannotRetireWorkingConnection() {
    host.connect("good", false).join();
    SdkSession original = host.getSession();
    org.mockito.Mockito.when(host.api.client.getServerInformation("malformed"))
        .thenReturn(
            CompletableFuture.completedFuture(
                GSON.fromJson("{\"account\":{}}", ServerInformation.class)));
    assertThrows(CompletionException.class, () -> host.connect("malformed", false).join());
    assertSame(original, host.getSession());
    assertFalse(original.isClosed());
  }

  @Test
  void authenticationAfterShutdownDoesNotSendRequests() {
    host.shutdown();
    assertTrue(host.connect("good", false).isCompletedExceptionally());
    org.mockito.Mockito.verify(host.api.client, org.mockito.Mockito.never())
        .getServerInformation(org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  void pluginReportingHonorsLocalAndRemoteOptOut() {
    host.connect("good", false).join();
    host.config.setAutoReportEnabled(true);
    host.createPluginEvent(PluginEvent.Level.WARNING, "not collected");
    assertTrue(host.snapshotPluginEvents().isEmpty());
    host.setStoreInfo(
        GSON.fromJson(
            "{\"account\":{\"log_events\":true},\"server\":{}}", ServerInformation.class));
    host.createPluginEvent(PluginEvent.Level.WARNING, "collected");
    assertEquals(1, host.snapshotPluginEvents().size());
    host.config.setAutoReportEnabled(false);
    assertTrue(host.snapshotPluginEvents().isEmpty());
  }

  @Test
  void shutdownCompletesPendingQueueChecksBeforeNetworkReturns() {
    host.connect("good", false).join();
    host.api.pendingDue = new CompletableFuture<>();
    CompletableFuture<String[]> check = host.checkCommandQueue();
    host.shutdown();
    assertTrue(check.isDone());
    host.api.pendingDue.complete(due());
    assertTrue(host.executed.isEmpty());
  }

  @Test
  void offlinePlayersAreRememberedForJoinWithoutFetchingTheirOnlineCommands() {
    host.connect("good", false).join();
    host.online = false;
    host.checkCommandQueue().join();
    assertFalse(host.getQueuedPlayers().isEmpty());
    org.mockito.Mockito.verify(host.api.client, org.mockito.Mockito.never())
        .getOnlineCommands(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
  }

  private static ServerInformation info() {
    return GSON.fromJson(
        "{\"account\":{\"id\":1,\"name\":\"Shop\",\"game_type\":\"Minecraft: Java Edition\",\"currency\":{\"iso_4217\":\"USD\",\"symbol\":\"$\"}},\"server\":{\"id\":1,\"name\":\"Test\"},\"public_token\":\"public\"}",
        ServerInformation.class);
  }

  private static DuePlayersResponse due() {
    return GSON.fromJson(
        "{\"meta\":{\"next_check\":120,\"execute_offline\":false},\"players\":[{\"id\":1,\"name\":\"Steve\",\"uuid\":\"123456781234123412341234567890ab\"}]}",
        DuePlayersResponse.class);
  }

  private static QueuedCommand command(int id, int delay, boolean online, int slots) {
    QueuedCommand command =
        GSON.fromJson(
            "{\"id\":"
                + id
                + ",\"command\":\"say {name}\",\"conditions\":{\"delay\":"
                + delay
                + ",\"slots\":"
                + slots
                + "}}",
            QueuedCommand.class);
    if (online) command.bindOnlinePlayer(due().getPlayers().get(0));
    return command;
  }

  private static QueuedCommand queuedCommand(int id, String command, int payment, int packageId) {
    QueuedCommand queued =
        GSON.fromJson(
            "{\"id\":"
                + id
                + ",\"command\":"
                + GSON.toJson(command)
                + ",\"payment\":"
                + payment
                + ",\"package\":"
                + packageId
                + ",\"player\":{\"id\":1,\"name\":\"Steve\",\"uuid\":\"123456781234123412341234567890ab\"}}",
            QueuedCommand.class);
    queued.bindOnlinePlayer(due().getPlayers().get(0));
    return queued;
  }

  static final class Host extends MockPluginPlatform {
    final Api api;
    final TestClock clock;
    final List<String> executed = new ArrayList<>();
    final List<Runnable> deferred = new ArrayList<>();
    final List<String> logs = new ArrayList<>();
    boolean online = true, failDispatch, defer;
    int slots = 36;

    Host() {
      this(new Api(), new TestClock());
    }

    private Host(Api api, TestClock clock) {
      super(key -> new SdkSession(key, api.client), clock);
      this.api = api;
      this.clock = clock;
    }

    public boolean isPlayerOnline(Object player) {
      return online;
    }

    public int getFreeSlots(Object player) {
      return slots;
    }

    public CommandResult dispatchCommand(String command) {
      executed.add(command);
      return CommandResult.from(!failDispatch);
    }

    public void executeBlocking(Runnable action) {
      if (defer) deferred.add(action);
      else action.run();
    }

    @Override
    public void log(java.util.logging.Level level, String message) {
      logs.add(message);
    }
  }

  static final class TestClock implements RuntimeClock {
    long now;

    @Override
    public long millis() {
      return now;
    }
  }

  static final class Api {
    final PluginApi client = org.mockito.Mockito.mock(PluginApi.class);

    Api() {
      org.mockito.Mockito.when(
              client.getServerInformation(org.mockito.ArgumentMatchers.anyString()))
          .thenAnswer(call -> getServerInformation(call.getArgument(0)));
      org.mockito.Mockito.when(client.getDuePlayers(org.mockito.ArgumentMatchers.anyString()))
          .thenAnswer(call -> getDuePlayers(call.getArgument(0)));
      org.mockito.Mockito.when(
              client.getOnlineCommands(
                  org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
          .thenAnswer(call -> getOnlineCommands(call.getArgument(0), call.getArgument(1)));
      org.mockito.Mockito.when(client.getListing(org.mockito.ArgumentMatchers.anyString()))
          .thenAnswer(call -> getListing(call.getArgument(0)));
      org.mockito.Mockito.when(
              client.deleteCommands(
                  org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyList()))
          .thenAnswer(call -> deleteCommands(call.getArgument(0), call.getArgument(1)));
      org.mockito.Mockito.when(client.sendPluginEvents(org.mockito.ArgumentMatchers.anyList()))
          .thenReturn(CompletableFuture.completedFuture(true));
      org.mockito.Mockito.when(
              client.checkForUpdate(
                  org.mockito.ArgumentMatchers.anyString(),
                  org.mockito.ArgumentMatchers.anyString()))
          .thenAnswer(call -> checkForUpdate(call.getArgument(0), call.getArgument(1)));
    }

    List<QueuedCommand> commands = Collections.emptyList();
    List<Integer> deleted = new ArrayList<>();
    boolean acknowledge = true;
    int deleteAttempts;
    CompletableFuture<DuePlayersResponse> pendingDue;
    CompletableFuture<ServerInformation> pendingAuth;
    PluginVersion pluginVersion;
    String updatePlatform;
    String runningVersion;

    public CompletableFuture<PluginVersion> checkForUpdate(String platform, String currentVersion) {
      updatePlatform = platform;
      runningVersion = currentVersion;
      return CompletableFuture.completedFuture(pluginVersion);
    }

    public CompletableFuture<ServerInformation> getServerInformation(String key) {
      if (key.equals("slow")) return pendingAuth;
      if (key.equals("bad")) {
        CompletableFuture<ServerInformation> f = new CompletableFuture<>();
        f.completeExceptionally(new IllegalArgumentException("Rejected"));
        return f;
      }
      return CompletableFuture.completedFuture(info());
    }

    public CompletableFuture<DuePlayersResponse> getDuePlayers(String key) {
      return pendingDue != null ? pendingDue : CompletableFuture.completedFuture(due());
    }

    public CompletableFuture<List<QueuedCommand>> getOnlineCommands(
        String key, QueuedPlayer player) {
      return CompletableFuture.completedFuture(commands);
    }

    public CompletableFuture<List<Category>> getListing(String key) {
      return CompletableFuture.completedFuture(Collections.emptyList());
    }

    public CompletableFuture<Boolean> deleteCommands(String key, List<Integer> ids) {
      deleteAttempts++;
      if (acknowledge) deleted.addAll(ids);
      return CompletableFuture.completedFuture(acknowledge);
    }

    public CompletableFuture<Boolean> sendPluginEvents(List<PluginEvent> events) {
      return CompletableFuture.completedFuture(true);
    }
  }
}
