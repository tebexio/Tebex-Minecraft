package io.tebex.plugin.util;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ItemUtil {
    private static final Map<String, Item> ITEM_CACHE = new HashMap<>();

    public static Optional<Item> fromString(String material) {
        if (ITEM_CACHE.containsKey(material)) {
            return Optional.of(ITEM_CACHE.get(material));
        }

        if (material.contains(":")) {
            String namespace = material.split(":")[0];
            String itemName = material.split(":")[1];
            Identifier id = new Identifier(namespace, itemName);

            Optional<Item> item = Registries.ITEM.getOrEmpty(id);
            item.ifPresent(value -> ITEM_CACHE.put(material, value));

            if (item.isEmpty()) { // attempt block item lookup
                Optional<Block> blockItem = Registries.BLOCK.getOrEmpty(id);
                if (blockItem.isPresent()) {
                    item = Optional.of(blockItem.get().asItem());
                    ITEM_CACHE.put(material, item.get());
                }
            }
            return item;
        }

        // no namespace in material identifier
        return Optional.empty();
    }
}
