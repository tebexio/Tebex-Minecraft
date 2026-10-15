package io.tebex.plugin.gui;

import io.tebex.minecraft.shop.ShopPresenter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Bukkit item metadata is distinct from Minecraft's NBT/data-component APIs. */
public final class ItemRenderer {
  private ItemRenderer() {}

  public static ItemStack create(Material material, ShopPresenter.Item spec) {
    ItemStack stack = new ItemStack(material);
    ItemMeta meta = stack.getItemMeta();
    if (meta == null) return stack;
    meta.setDisplayName(spec.name);
    meta.setLore(spec.lore);
    hideFlagsWhenSupported(meta);
    if (spec.enchanted) meta.addEnchant(Enchantment.PROTECTION_FIRE, 1, false);
    stack.setItemMeta(meta);
    return stack;
  }

  private static void hideFlagsWhenSupported(ItemMeta meta) {
    try {
      ClassLoader loader = ItemRenderer.class.getClassLoader();
      Class<?> flagType = Class.forName("org.bukkit.inventory.ItemFlag", false, loader);
      Class<?> metaType = Class.forName("org.bukkit.inventory.meta.ItemMeta", false, loader);
      List<Object> flags = new ArrayList<>();
      for (String name : new String[] {"HIDE_ATTRIBUTES", "HIDE_ENCHANTS", "HIDE_UNBREAKABLE"}) {
        try {
          Field field = flagType.getField(name);
          flags.add(field.get(null));
        } catch (NoSuchFieldException ignored) {
          // Older Bukkit APIs may expose only a subset of the item flags.
        }
      }
      if (flags.isEmpty()) return;

      Object flagArray = Array.newInstance(flagType, flags.size());
      for (int index = 0; index < flags.size(); index++) {
        Array.set(flagArray, index, flags.get(index));
      }
      Method addItemFlags = metaType.getMethod("addItemFlags", flagArray.getClass());
      addItemFlags.invoke(meta, flagArray);
    } catch (ReflectiveOperationException | LinkageError ignored) {
      // ItemFlag and addItemFlags were added after Bukkit 1.7; hiding flags is cosmetic.
    }
  }
}
