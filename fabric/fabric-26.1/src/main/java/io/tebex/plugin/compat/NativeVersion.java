package io.tebex.plugin.compat;

import io.tebex.minecraft.shop.ShopPresenter;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.block.Block;

/** Small, explicitly selected Minecraft-version API boundary. No commerce or lifecycle policy. */
public final class NativeVersion {
  private NativeVersion() {}

  public static void dispatch(MinecraftServer server, String command) {
    CommandSourceStack source = server.createCommandSourceStack();
    server.getCommands().performCommand(source.dispatcher().parse(command, source), command);
  }

  public static List<ItemStack> inventory(ServerPlayer player) {
    return player.getInventory().getNonEquipmentItems();
  }

  public static boolean isOp(ServerPlayer player) {
    return player != null
        && player.level().getServer().getPlayerList().isOp(new NameAndId(player.getGameProfile()));
  }

  public static boolean hasAdminPermission(ServerPlayer player) {
    return player != null && player.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
  }

  public static Optional<Item> findItem(String name) {
    Identifier id = Identifier.tryParse(name);
    if (id == null) return Optional.empty();
    Optional<Item> item = BuiltInRegistries.ITEM.getOptional(id);
    return item.isPresent() ? item : BuiltInRegistries.BLOCK.getOptional(id).map(Block::asItem);
  }

  public static Component checkoutLink(String url) {
    return Component.literal("Checkout started! Complete payment here: ")
        .append(
            Component.literal(url)
                .withStyle(
                    style ->
                        style
                            .withUnderlined(true)
                            .withClickEvent(new ClickEvent.OpenUrl(java.net.URI.create(url)))));
  }

  public static ItemStack createItem(Item material, ShopPresenter.Item spec) {
    ItemStack stack = new ItemStack(material);
    stack.set(DataComponents.CUSTOM_NAME, Component.nullToEmpty(spec.name));
    ItemLore lore = ItemLore.EMPTY;
    for (String line : spec.lore) lore = lore.withLineAdded(Component.nullToEmpty(line));
    stack.set(DataComponents.LORE, lore);
    if (spec.enchanted) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
    return stack;
  }
}
