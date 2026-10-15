package io.tebex.plugin.compat;

import io.tebex.minecraft.util.MaterialResolver;
import java.util.Optional;
import net.minecraft.util.datafix.fixes.ItemIdFix;
import net.minecraft.world.item.Item;

/** Variant-local registry adapter backed by the platform-neutral material resolver. */
public final class ItemUtil {
  private static final MaterialResolver<Item> ITEMS =
      new MaterialResolver<>(NativeVersion::findItem, ItemIdFix::getItem);

  private ItemUtil() {}

  public static Optional<Item> fromString(String value) {
    return ITEMS.resolve(value);
  }
}
