package io.tebex.minecraft.runtime;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import io.tebex.model.QueuedCommand;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class DeliveryQueueTest {
  private QueuedCommand command(int id, int delay) {
    return new Gson()
        .fromJson(
            "{\"id\":" + id + ",\"command\":\"say test\",\"conditions\":{\"delay\":" + delay + "}}",
            QueuedCommand.class);
  }

  @Test
  void delaysDoNotBlockImmediateCommandsAndDuplicatesDoNotAccumulate() {
    AtomicLong clock = new AtomicLong();
    List<Integer> applied = new ArrayList<>();
    DeliveryQueue queue =
        new DeliveryQueue(
            clock::get,
            c -> {
              applied.add(c.getId());
              return CompletableFuture.completedFuture(true);
            });
    queue.offer(command(1, 30));
    queue.offer(command(1, 30));
    queue.offer(command(2, 0));
    queue.pump();
    assertEquals(Arrays.asList(2), applied);
    clock.set(30000);
    queue.pump();
    queue.pump();
    assertEquals(Arrays.asList(2, 1), applied);
  }

  @Test
  void waitsForActualAsyncDispatchCompletion() {
    List<Integer> applied = new ArrayList<>();
    CompletableFuture<Boolean> first = new CompletableFuture<>();
    DeliveryQueue queue =
        new DeliveryQueue(
            () -> 0L,
            c -> {
              applied.add(c.getId());
              return c.getId() == 1 ? first : CompletableFuture.completedFuture(true);
            });
    queue.offer(command(1, 0));
    queue.offer(command(2, 0));
    queue.pump();
    queue.pump();
    assertEquals(Arrays.asList(1), applied);
    assertTrue(queue.acknowledgements().isEmpty());
    first.complete(true);
    queue.pump();
    assertEquals(Arrays.asList(1, 2), applied);
  }

  @Test
  void acknowledgementFailureDoesNotRedispatchEvenAfterLaterSuccessfulAcknowledgement() {
    List<Integer> applied = new ArrayList<>();
    DeliveryQueue queue =
        new DeliveryQueue(
            () -> 0L,
            c -> {
              applied.add(c.getId());
              return CompletableFuture.completedFuture(true);
            });
    queue.offer(command(1, 0));
    queue.pump();
    assertEquals(Arrays.asList(1), queue.acknowledgements());
    queue.offer(command(1, 0));
    queue.pump();
    assertEquals(Arrays.asList(1), queue.acknowledgements());
    queue.acknowledged(Arrays.asList(1));
    queue.offer(command(1, 0));
    queue.pump();
    assertEquals(Arrays.asList(1), applied);
    assertTrue(queue.acknowledgements().isEmpty());
  }

  @Test
  void skippedDeliveryCanBeRetriedButIsNeverAcknowledged() {
    CompletableFuture<Boolean> outcome = new CompletableFuture<>();
    DeliveryQueue queue = new DeliveryQueue(() -> 0L, c -> outcome);
    queue.offer(command(1, 0));
    queue.pump();
    outcome.complete(false);
    assertTrue(queue.acknowledgements().isEmpty());
    queue.offer(command(1, 0));
    queue.pump();
    assertTrue(queue.acknowledgements().isEmpty());
  }

  @Test
  void closedQueueRejectsLateDispatchCompletion() {
    CompletableFuture<Boolean> outcome = new CompletableFuture<>();
    DeliveryQueue queue = new DeliveryQueue(() -> 0L, c -> outcome);
    queue.offer(command(1, 0));
    queue.pump();
    queue.close();
    outcome.complete(true);
    queue.offer(command(2, 0));
    queue.pump();
    assertTrue(queue.acknowledgements().isEmpty());
  }
}
