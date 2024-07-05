package io.tebex.plugin.gui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

public class TebexGuiItem {
    private TebexGuiAction<InventoryClickEvent> action;
    private ItemStack stack;

    public TebexGuiItem(ItemStack stack, TebexGuiAction<InventoryClickEvent> action) {
        this.action = action;
        this.stack = stack;
    }

    public ItemStack getStack() {
        return stack;
    }

    @Override
    public String toString() {
        return this.stack.toString();
    }
}
