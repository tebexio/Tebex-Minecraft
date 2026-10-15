package io.tebex.minecraft.runtime;

import io.tebex.http.PluginApi;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/** An authenticated, cancellable lifetime around the official SDK client. */
public final class SdkSession {
  private final String secretKey;
  private final PluginApi api;
  private final Set<CompletableFuture<?>> pending =
      java.util.concurrent.ConcurrentHashMap.newKeySet();
  private volatile boolean closed;

  public SdkSession(String secretKey) {
    this(secretKey, new PluginApi());
  }

  SdkSession(String secretKey, PluginApi api) {
    this.secretKey = secretKey == null ? "" : secretKey;
    this.api = Objects.requireNonNull(api, "api");
  }

  public String getSecretKey() {
    return secretKey;
  }

  public boolean isClosed() {
    return closed;
  }

  public void close() {
    closed = true;
    pending.forEach(
        future -> future.completeExceptionally(new CancellationException("Connection retired")));
  }

  /** Runs an operation directly on the SDK using this session's credential. */
  public <T> CompletableFuture<T> request(
      BiFunction<PluginApi, String, CompletableFuture<T>> operation) {
    if (closed) return cancelled();

    CompletableFuture<T> response;
    try {
      response =
          Objects.requireNonNull(operation.apply(api, secretKey), "SDK operation returned null")
              .thenApply(
                  value -> {
                    if (closed) throw new CancellationException("Connection retired");
                    return value;
                  });
    } catch (RuntimeException error) {
      response = new CompletableFuture<>();
      response.completeExceptionally(error);
    }

    pending.add(response);
    CompletableFuture<T> tracked = response;
    response.whenComplete((value, error) -> pending.remove(tracked));
    if (closed) response.completeExceptionally(new CancellationException("Connection retired"));
    return response;
  }

  private static <T> CompletableFuture<T> cancelled() {
    CompletableFuture<T> rejected = new CompletableFuture<>();
    rejected.completeExceptionally(new CancellationException("Connection retired"));
    return rejected;
  }
}
