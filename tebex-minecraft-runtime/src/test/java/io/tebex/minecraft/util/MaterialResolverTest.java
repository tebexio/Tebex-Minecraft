package io.tebex.minecraft.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class MaterialResolverTest {
  @Test
  void normalizesCachesAndSupportsLegacyIdsWithoutCachingMisses() {
    AtomicInteger lookups = new AtomicInteger();
    MaterialResolver<String> resolver =
        new MaterialResolver<>(
            id -> {
              lookups.incrementAndGet();
              return id.equals("minecraft:stone") || id.equals("mod:test")
                  ? Optional.of(id)
                  : Optional.empty();
            },
            number -> number == 1 ? "minecraft:stone" : "minecraft:air");
    assertEquals("minecraft:stone", resolver.resolve(" STONE ").get());
    assertEquals("minecraft:stone", resolver.resolve("stone").get());
    assertEquals(1, lookups.get());
    assertEquals("minecraft:stone", resolver.resolve("1:0").get());
    assertEquals("mod:test", resolver.resolve("MOD:TEST").get());
    assertFalse(resolver.resolve("999999999999999999999999").isPresent());
    assertFalse(resolver.resolve("0").isPresent());
    assertFalse(resolver.resolve(null).isPresent());
  }

  @Test
  void usesLocaleIndependentIdentifiers() {
    Locale previous = Locale.getDefault();
    try {
      Locale.setDefault(new Locale("tr", "TR"));
      assertEquals(
          "minecraft:iron_block",
          new MaterialResolver<String>(Optional::of, id -> null).resolve("IRON BLOCK").get());
    } finally {
      Locale.setDefault(previous);
    }
  }
}
