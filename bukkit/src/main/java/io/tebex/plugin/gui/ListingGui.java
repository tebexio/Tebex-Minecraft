package io.tebex.plugin.gui;

import io.tebex.minecraft.shop.MenuActions;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** One inventory allocation; the shared action map enforces bounds and one-shot navigation. */
public final class ListingGui implements InventoryHolder {
  private final Inventory inventory;
  private final Map<Integer, Runnable> actions = new HashMap<>();
  private MenuActions menuActions;

  public ListingGui(int rows, String title) {
    inventory = Bukkit.createInventory(this, rows * 9, title);
  }

  @Override
  public Inventory getInventory() {
    return inventory;
  }

  public void addItem(int slot, ItemStack item, Runnable action) {
    inventory.setItem(slot, item);
    actions.put(slot, action);
  }

  public void open(Player player) {
    menuActions = new MenuActions(inventory.getSize(), actions);
    player.openInventory(inventory);
  }

  public void select(int slot) {
    if (menuActions != null) menuActions.select(slot);
  }
}
