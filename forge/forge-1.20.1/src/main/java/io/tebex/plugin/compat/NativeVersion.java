package io.tebex.plugin.compat;

import io.tebex.minecraft.shop.ShopPresenter;
import java.util.List;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;

/** Small, explicitly selected Minecraft-version API boundary. No commerce or lifecycle policy. */
public final class NativeVersion {
  private NativeVersion() {}

  public static void dispatch(MinecraftServer server, String command) {
    CommandSourceStack source = server.createCommandSourceStack();
    server.getCommands().performPrefixedCommand(source, command);
  }

  public static List<ItemStack> inventory(ServerPlayer player) {
    return player.getInventory().items;
  }

  public static boolean isOp(ServerPlayer player) {
    return player != null
        && player.level().getServer().getPlayerList().isOp(player.getGameProfile());
  }

  public static boolean hasAdminPermission(ServerPlayer player) {
    return player != null && player.hasPermissions(4);
  }

  public static Optional<Item> findItem(String name) {
    ResourceLocation id = ResourceLocation.tryParse(name);
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
                            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))));
  }

  public static ItemStack createItem(Item material, ShopPresenter.Item spec) {
    ItemStack stack = new ItemStack(material);
    stack.setHoverName(Component.nullToEmpty(spec.name));
    ListTag lore = new ListTag();
    for (String line : spec.lore)
      lore.add(StringTag.valueOf(Component.Serializer.toJson(Component.nullToEmpty(line))));
    stack.getOrCreateTagElement("display").put("Lore", lore);
    stack.getOrCreateTag().putInt("HideFlags", 7);
    if (spec.enchanted) stack.enchant(Enchantments.UNBREAKING, 1);
    return stack;
  }
}
