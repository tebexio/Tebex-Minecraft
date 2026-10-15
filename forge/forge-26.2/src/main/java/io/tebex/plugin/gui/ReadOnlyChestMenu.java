package io.tebex.plugin.gui;

import io.tebex.minecraft.shop.MenuActions;
import java.util.Map;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** Never delegate shop input to vanilla's item-transfer implementation. */
public abstract class ReadOnlyChestMenu extends ChestMenu {
  private final MenuActions actions;

  protected ReadOnlyChestMenu(
      MenuType<?> type,
      int id,
      Inventory playerInventory,
      Container inventory,
      int rows,
      Map<Integer, Runnable> actions) {
    super(type, id, playerInventory, inventory, rows);
    this.actions = new MenuActions(rows * 9, actions);
  }

  protected final void select(int slot) {
    actions.select(slot);
  }

  @Override
  public ItemStack quickMoveStack(Player player, int slot) {
    return ItemStack.EMPTY;
  }

  @Override
  public boolean stillValid(Player player) {
    return true;
  }
}
