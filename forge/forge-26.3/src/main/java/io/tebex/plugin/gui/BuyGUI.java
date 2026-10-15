package io.tebex.plugin.gui;

import io.tebex.minecraft.runtime.TebexRuntime;
import io.tebex.minecraft.shop.ShopPresenter;
import io.tebex.plugin.compat.ItemUtil;
import io.tebex.plugin.compat.NativeVersion;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** One native renderer for all mod loaders; only item encoding varies by Minecraft version. */
public final class BuyGUI {
  private final TebexRuntime platform;

  public BuyGUI(TebexRuntime platform) {
    this.platform = platform;
  }

  public void open(ServerPlayer player) {
    new ShopPresenter(platform)
        .open(
            player.getName().getString(),
            menu -> {
              ListingGui gui = new ListingGui(menu.rows, menu.title, player);
              menu.items.forEach(
                  (slot, spec) -> {
                    Item fallback = ItemUtil.fromString(spec.fallback).orElse(Items.BOOK);
                    Item material = ItemUtil.fromString(spec.material).orElse(fallback);
                    gui.addItem(slot, NativeVersion.createItem(material, spec), spec.action);
                  });
              platform.executeForPlayer(player.getUUID(), gui::open, () -> {});
            },
            player::closeContainer);
  }
}
