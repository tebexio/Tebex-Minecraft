package io.tebex.plugin.gui;

import java.util.Map;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;

/** Minecraft's input enum is the only version-specific container API. */
public final class TebexBuyScreenHandler extends ReadOnlyChestMenu {
  public TebexBuyScreenHandler(
      MenuType<?> type,
      int id,
      Inventory playerInventory,
      Container inventory,
      int rows,
      Map<Integer, Runnable> actions) {
    super(type, id, playerInventory, inventory, rows, actions);
  }

  @Override
  public void clicked(int slot, int button, ContainerInput input, Player player) {
    if (input == ContainerInput.PICKUP) select(slot);
  }
}
