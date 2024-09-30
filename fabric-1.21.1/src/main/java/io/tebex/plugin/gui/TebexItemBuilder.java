package io.tebex.plugin.gui;

import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Unit;

import java.util.List;

public class TebexItemBuilder {
    private TebexGuiItem guiItem;

    private String displayName;
    private Item material;
    private List<String> lore;
    private ComponentType[] hideFlags;
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
            stack.set(DataComponentTypes.LORE, LoreComponent.DEFAULT.with(Text.of(loreEntry)));
        });

        stack.set(DataComponentTypes.CUSTOM_NAME, Text.of(this.displayName));
        //FIXME
//        for (DataComponentTypes tooltipSection : hideFlags) {
//            stack.set(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP, List.of(tooltipSection));
//        }

        if (isEnchanted) {
            stack.set(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
        }

        return stack;
    }

    public void enchant() {
        this.isEnchanted = true;
    }

    public TebexItemBuilder hideFlags(ComponentType... itemFlags) {
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
