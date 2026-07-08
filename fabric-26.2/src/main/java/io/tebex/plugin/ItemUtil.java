package io.tebex.plugin;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.fixes.ItemIdFix;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ItemUtil {
    private static final Map<String, Item> ITEM_CACHE = new HashMap<>();

    public static Optional<Item> fromString(String material) {
        if (material == null || material.isBlank()) {
            return Optional.empty();
        }

        String cacheKey = material.trim();
        if (ITEM_CACHE.containsKey(cacheKey)) {
            return Optional.of(ITEM_CACHE.get(cacheKey));
        }

        Optional<Item> item = toMinecraftIdentifier(cacheKey).flatMap(ItemUtil::resolveItem);
        if (item.isEmpty()) {
            item = toCompatibleIdentifier(cacheKey).flatMap(ItemUtil::resolveItem);
        }

        item.ifPresent(value -> ITEM_CACHE.put(cacheKey, value));
        return item;
    }

    private static Optional<Item> resolveItem(Identifier id) {
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
        if (item.isPresent()) {
            return item;
        }

        Optional<Block> block = BuiltInRegistries.BLOCK.getOptional(id);
        return block.map(Block::asItem);
    }

    private static Optional<Identifier> toMinecraftIdentifier(String material) {
        String normalized = material.toLowerCase(Locale.ROOT).replace(' ', '_');
        if (!normalized.contains(":")) {
            normalized = "minecraft:" + normalized;
        }

        return Optional.ofNullable(Identifier.tryParse(normalized));
    }

    private static Optional<Identifier> toCompatibleIdentifier(String material) {
        String normalized = material.trim();
        String[] split = normalized.split(":", 2);

        if (split[0].matches("\\d+")) {
            return fromLegacyNumericId(split[0]);
        }

        if (split.length == 2 && split[0].equalsIgnoreCase("minecraft")) {
            return Optional.ofNullable(Identifier.tryParse("minecraft:" + split[1].toLowerCase(Locale.ROOT)));
        }

        if (split.length == 1) {
            return Optional.ofNullable(Identifier.tryParse("minecraft:" + normalized.toLowerCase(Locale.ROOT).replace(' ', '_')));
        }

        return Optional.empty();
    }

    private static Optional<Identifier> fromLegacyNumericId(String id) {
        try {
            String itemId = ItemIdFix.getItem(Integer.parseInt(id));
            if ("minecraft:air".equals(itemId)) {
                return Optional.empty();
            }

            return Optional.ofNullable(Identifier.tryParse(itemId));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
