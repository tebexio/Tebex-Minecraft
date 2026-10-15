package io.tebex.plugin.event;

import static org.mockito.Mockito.*;

import io.tebex.plugin.gui.ListingGui;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.Test;

class InventoryClickListenerTest {
  private final InventoryClickListener listener = new InventoryClickListener();

  @Test
  void ordinaryShopClickIsCancelledAndRouted() {
    ListingGui gui = mock(ListingGui.class);
    Inventory inventory = mock(Inventory.class);
    when(inventory.getHolder()).thenReturn(gui);
    InventoryClickEvent event = mock(InventoryClickEvent.class);
    when(event.getInventory()).thenReturn(inventory);
    when(event.getClick()).thenReturn(ClickType.LEFT);
    when(event.getRawSlot()).thenReturn(4);
    listener.onInventoryClick(event);
    verify(event).setCancelled(true);
    verify(gui).select(4);
  }

  @Test
  void shiftClicksCannotTransferShopItemsOrNavigate() {
    ListingGui gui = mock(ListingGui.class);
    Inventory inventory = mock(Inventory.class);
    when(inventory.getHolder()).thenReturn(gui);
    InventoryClickEvent event = mock(InventoryClickEvent.class);
    when(event.getInventory()).thenReturn(inventory);
    when(event.getClick()).thenReturn(ClickType.SHIFT_LEFT);
    listener.onInventoryClick(event);
    verify(event).setCancelled(true);
    verify(gui, never()).select(anyInt());
  }

  @Test
  void draggingIntoShopIsCancelledButUnrelatedInventoriesAreUntouched() {
    Inventory inventory = mock(Inventory.class);
    InventoryDragEvent event = mock(InventoryDragEvent.class);
    when(event.getInventory()).thenReturn(inventory);
    listener.onInventoryDrag(event);
    verify(event, never()).setCancelled(anyBoolean());
    when(inventory.getHolder()).thenReturn(mock(ListingGui.class));
    listener.onInventoryDrag(event);
    verify(event).setCancelled(true);
  }
}
