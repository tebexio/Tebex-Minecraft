package io.tebex.plugin.gui;

import io.tebex.minecraft.shop.ShopPresenter;
import io.tebex.plugin.FoliaPlatform;
import io.tebex.plugin.compat.MaterialUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Bukkit inventory renderer for the shared shop presenter. */
public final class BuyGUI {
  private final FoliaPlatform platform;

  public BuyGUI(FoliaPlatform platform) {
    this.platform = platform;
  }

  public void open(Player player) {
    new ShopPresenter(platform.runtime())
        .open(
            player.getName(),
            menu -> {
              ListingGui gui = new ListingGui(menu.rows, menu.title);
              menu.items.forEach(
                  (slot, spec) -> {
                    Material fallback =
                        MaterialUtil.fromString(spec.fallback).orElse(Material.BOOK);
                    Material material =
                        spec.material == null
                            ? fallback
                            : MaterialUtil.fromString(spec.material).orElse(fallback);
                    gui.addItem(slot, ItemRenderer.create(material, spec), spec.action);
                  });
              platform
                  .runtime()
                  .executeForPlayer(player.getUniqueId(), () -> gui.open(player), () -> {});
            },
            player::closeInventory);
  }
}
