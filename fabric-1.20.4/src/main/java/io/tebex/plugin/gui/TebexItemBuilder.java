package io.tebex.plugin.gui;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;

import java.util.List;

public class TebexItemBuilder {
    private TebexGuiItem guiItem;

    private String displayName;
    private Item material;
    private List<String> lore;
    private ItemStack.TooltipSection[] hideFlags;
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
        NbtCompound displayTag = stack.getOrCreateSubNbt("display");

        NbtList loreList = new NbtList();
        lore.forEach(loreEntry -> {
            loreList.add(NbtString.of(Text.Serialization.toJsonString(Text.of(loreEntry))));
        });
        displayTag.put(ItemStack.LORE_KEY, loreList);
        displayTag.putString("Name", Text.Serialization.toJsonString(Text.of(this.displayName)));
        for (ItemStack.TooltipSection tooltipSection : hideFlags) {
            stack.addHideFlag(tooltipSection);
        }

        if (isEnchanted) {
            stack.addEnchantment(Enchantment.byRawId(0), 1);
        }

        stack.setSubNbt("display", displayTag);
        return stack;
    }

    public void enchant() {
        this.isEnchanted = true;
    }

    public TebexItemBuilder hideFlags(ItemStack.TooltipSection... itemFlags) {
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
