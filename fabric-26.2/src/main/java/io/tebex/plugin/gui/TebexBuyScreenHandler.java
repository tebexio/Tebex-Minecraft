package io.tebex.plugin.gui;

import java.util.HashMap;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;

public class TebexBuyScreenHandler extends ChestMenu {
    private boolean cancelled = false;
    private final HashMap<Integer, TebexGuiItem> guiItems;

    public TebexBuyScreenHandler(MenuType<?> type, int syncId, Inventory playerInventory, Container inventory, int rows, HashMap<Integer, TebexGuiItem> guiItems) {
        super(type, syncId, playerInventory, inventory, rows);
        this.guiItems = guiItems;
    }

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        if (cancelled) {
            return;
        }

        if (input != ContainerInput.PICKUP) { // Ignore non-pickup only actions
            return;
        }

        if (slotId > this.getContainer().getContainerSize()) { // Ignore slot clicks outsize of the buy inventory
            return;
        }

        if (slotId >= 0 && slotId < this.getContainer().getContainerSize()) {
            TebexGuiItem item = guiItems.get(slotId);
            if (item != null && item.getAction() != null) {
                item.getAction().execute(this);
            }
        }

        super.clicked(slotId, button, input, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public void setCancelled(boolean value) {
        cancelled = value;
    }
}
