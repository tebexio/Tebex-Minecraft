package io.tebex.plugin.gui;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import java.util.List;

public class TebexItemBuilder {
    private TebexGuiItem guiItem;

    private String displayName;
    private Item material;
    private List<String> lore;
    private DataComponentType[] hideFlags;
    private boolean isEnchanted;
    private TebexGuiAction<TebexBuyScreenHandler> action;

    private TebexItemBuilder(Item material, TebexGuiAction<TebexBuyScreenHandler> action) {
        this.material = material;
        this.action = action;
    }

    public static TebexItemBuilder from(Item material) {
        return new TebexItemBuilder(material, null);
    }

    public TebexGuiItem asGuiItem(TebexGuiAction<TebexBuyScreenHandler> clickAction) {
        return new TebexGuiItem(buildItemStack(), clickAction);
    }

    public ItemStack buildItemStack() {
        ItemStack stack = new ItemStack(this.material);

        lore.forEach(loreEntry -> {
            stack.set(DataComponents.LORE, ItemLore.EMPTY.withLineAdded(Component.nullToEmpty(loreEntry)));
        });

        stack.set(DataComponents.CUSTOM_NAME, Component.nullToEmpty(this.displayName));
        //FIXME
//        for (DataComponentTypes tooltipSection : hideFlags) {
//            stack.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, List.of(tooltipSection));
//        }

        if (isEnchanted) {
            stack.set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        }

        return stack;
    }

    public void enchant() {
        this.isEnchanted = true;
    }

    public TebexItemBuilder hideFlags(DataComponentType... itemFlags) {
        this.hideFlags = itemFlags;
        return this;
    }

    public TebexItemBuilder name(String name) {
        this.displayName = name;
        return this;
    }

    public TebexItemBuilder lore(List<String> lore) {
        this.lore = lore;
        return this;
    }

    @Override
    public String toString() {
        return "TebexItemBuilder{" +
                "displayName=" + displayName +
                ", material=" + material +
                ", lore='" +  lore.toString() +
                ", hideFlags=" + hideFlags.toString() +
                ", isEnchanted=" + isEnchanted +
                ", action=" + action +
                '}';
    }
}
