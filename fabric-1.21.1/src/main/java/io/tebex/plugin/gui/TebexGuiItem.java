package io.tebex.plugin.gui;


import net.minecraft.item.ItemStack;

public class TebexGuiItem {
    private TebexGuiAction<TebexBuyScreenHandler> action;
    private ItemStack stack;

    public TebexGuiItem(ItemStack stack, TebexGuiAction<TebexBuyScreenHandler> action) {
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

    public TebexGuiAction<TebexBuyScreenHandler> getAction() {
        return action;
    }
}
