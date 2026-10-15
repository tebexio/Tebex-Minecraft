package io.tebex.plugin.gui;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** A read-only chest presentation; no duplicate GUI item/action/builder model is needed. */
public final class ListingGui {
  private final SimpleContainer inventory;
  private final Map<Integer, Runnable> actions = new HashMap<>();
  private final int rows;
  private final String title;
  private final ServerPlayer player;

  public ListingGui(int rows, String title, ServerPlayer player) {
    this.rows = rows;
    this.title = title;
    this.player = player;
    inventory = new SimpleContainer(rows * 9);
  }

  public void addItem(int slot, ItemStack stack, Runnable action) {
    inventory.setItem(slot, stack);
    actions.put(slot, action);
  }

  public void open() {
    MenuType<?> type =
        switch (rows) {
          case 1 -> MenuType.GENERIC_9x1;
          case 2 -> MenuType.GENERIC_9x2;
          case 3 -> MenuType.GENERIC_9x3;
          case 4 -> MenuType.GENERIC_9x4;
          case 5 -> MenuType.GENERIC_9x5;
          default -> MenuType.GENERIC_9x6;
        };
    player.openMenu(
        new SimpleMenuProvider(
            (id, playerInventory, ignored) ->
                new TebexBuyScreenHandler(type, id, playerInventory, inventory, rows, actions),
            Component.literal(title)));
  }
}
