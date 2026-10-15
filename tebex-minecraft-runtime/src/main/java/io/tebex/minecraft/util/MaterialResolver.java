package io.tebex.minecraft.util;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.IntFunction;

/** Shared normalization, legacy numeric fallback and bounded positive-result cache. */
public final class MaterialResolver<T> {
  private final Function<String, Optional<T>> lookup;
  private final IntFunction<String> legacy;
  private final ConcurrentHashMap<String, T> cache = new ConcurrentHashMap<>();

  public MaterialResolver(Function<String, Optional<T>> lookup, IntFunction<String> legacy) {
    this.lookup = lookup;
    this.legacy = legacy;
  }

  public Optional<T> resolve(String input) {
    if (input == null || input.trim().isEmpty()) return Optional.empty();
    String key = input.trim().toLowerCase(Locale.ROOT).replace(' ', '_');
    T cached = cache.get(key);
    if (cached != null) return Optional.of(cached);
    String id = key.contains(":") ? key : "minecraft:" + key;
    Optional<T> result = lookup.apply(id);
    if (!result.isPresent()) {
      String numeric = key.split(":", 2)[0];
      if (numeric.matches("[0-9]+")) {
        try {
          String mapped = legacy.apply(Integer.parseInt(numeric));
          if (mapped != null && !mapped.equals("minecraft:air")) result = lookup.apply(mapped);
        } catch (NumberFormatException ignored) {
          /* Not a valid legacy numeric id. */
        }
      }
    }
    if (result.isPresent() && cache.size() < 1024) cache.putIfAbsent(key, result.get());
    return result;
  }
}
