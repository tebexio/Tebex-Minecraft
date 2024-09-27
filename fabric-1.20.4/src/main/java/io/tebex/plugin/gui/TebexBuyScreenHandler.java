package io.tebex.plugin.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;

import java.util.HashMap;

public class TebexBuyScreenHandler extends GenericContainerScreenHandler {
    private boolean cancelled = false;
    private final HashMap<Integer, TebexGuiItem> guiItems;

    public TebexBuyScreenHandler(ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, Inventory inventory, int rows, HashMap<Integer, TebexGuiItem> guiItems) {
        super(type, syncId, playerInventory, inventory, rows);
        this.guiItems = guiItems;
    }

    @Override
    public void onSlotClick(int slotId, int button, SlotActionType actionType, PlayerEntity player) {
        if (cancelled) {
            return;
        }

        if (slotId >= 0 && slotId < this.slots.size()) {
            ItemStack clickedStack = this.getSlot(slotId).getStack();
            TebexGuiItem item = guiItems.get(slotId);
            if (item != null && item.getAction() != null) {
                item.getAction().execute(this);
            }
        }

        super.onSlotClick(slotId, button, actionType, player);
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    public void setCancelled(boolean value) {
        cancelled = value;
    }
}
