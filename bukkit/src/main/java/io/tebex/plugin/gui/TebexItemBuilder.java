package io.tebex.plugin.gui;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TebexItemBuilder {
    private TebexGuiItem guiItem;

    private String displayName;
    private Material material;
    private List<String> lore;
    private ItemFlag[] flags;
    private boolean isEnchanted;
    private TebexGuiAction<InventoryClickEvent> action;

    private TebexItemBuilder(Material material, TebexGuiAction<InventoryClickEvent> action) {
        this.material = material;
        this.action = action;
    }

    public static TebexItemBuilder from(Material material) {
        return new TebexItemBuilder(material, null);
    }

    public TebexGuiItem asGuiItem(TebexGuiAction<InventoryClickEvent> clickAction) {
        return new TebexGuiItem(buildItemStack(), clickAction);
    }

    public ItemStack buildItemStack() {
        ItemStack stack = new ItemStack(this.material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setLore(lore);
        meta.addItemFlags(flags);
        if (isEnchanted) {
            meta.addEnchant(Enchantment.PROTECTION_FIRE, 1, false);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    public void enchant() {
        this.isEnchanted = true;
    }

    public TebexItemBuilder flags(ItemFlag... itemFlags) {
        this.flags = itemFlags;
        return this;
    }

    public TebexItemBuilder name(String name) {
        this.displayName = name;
        return this;
    }

    public TebexItemBuilder lore(List<String> lore) {
        this.lore = lore;
        return this;
    }
}
