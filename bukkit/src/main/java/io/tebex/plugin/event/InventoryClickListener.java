package io.tebex.plugin.event;

import io.tebex.plugin.gui.ListingGui;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public final class InventoryClickListener implements Listener {
  @EventHandler
  public void onInventoryClick(InventoryClickEvent event) {
    if (!(event.getInventory().getHolder() instanceof ListingGui)) return;
    event.setCancelled(true);
    if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT)
      ((ListingGui) event.getInventory().getHolder()).select(event.getRawSlot());
  }

  @EventHandler
  public void onInventoryDrag(InventoryDragEvent event) {
    if (event.getInventory().getHolder() instanceof ListingGui) event.setCancelled(true);
  }
}
