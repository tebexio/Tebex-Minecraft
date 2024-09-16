package io.tebex.plugin.event;

import io.tebex.plugin.gui.ListingGui;
import io.tebex.plugin.gui.TebexGuiItem;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class InventoryClickListener implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (!(inventory.getHolder() instanceof ListingGui)) {
            return;
        }
        ListingGui listingGui = (ListingGui) inventory.getHolder();

        event.setCancelled(true); // Cancel the default click behavior

        int slot = event.getRawSlot();
        TebexGuiItem guiItem = listingGui.getItemInSlot(slot);
        if (guiItem != null && guiItem.getAction() != null) {
            guiItem.getAction().execute(event);  // Invoke the action
        }
    }
}
