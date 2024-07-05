package io.tebex.plugin.gui;

import io.tebex.sdk.Tebex;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ListingGui implements InventoryHolder {
    private Inventory inventory;

    private String title;
    private int rows = 4;
    private ArrayList<String> lore;
    private HashMap<Integer, TebexGuiItem> items;

    public ListingGui() {
        this.inventory = Bukkit.createInventory(this, rows*9, "");
        this.lore = new ArrayList<>();
        this.items = new HashMap<>();
    }

    public ListingGui title(String title) {
        this.title = title;
        return this;
    }

    public ListingGui rows(int rows) {
        return this;
    }

    public ListingGui lore(ArrayList<String> lore) {
        this.lore = lore;
        return this;
    }

    public ListingGui create() {
        this.inventory = Bukkit.createInventory(this, rows*9, this.title);
        return this;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public int getRows() {
        return this.rows;
    }

    public void addItem(TebexGuiItem guiItem) {
        this.items.put(this.items.size(), guiItem);
    }

    public void open(Player player)
    {
        this.inventory.clear();

        for (Map.Entry<Integer,TebexGuiItem> guiItems : items.entrySet()) {

            TebexGuiItem guiItem = guiItems.getValue();


            ItemStack stack = guiItem.getStack();
            ItemMeta meta = stack.getItemMeta();

            if (lore.size() > 0) {
                meta.setLore(this.lore);
            }
            
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
            stack.setItemMeta(meta);

            Tebex.get().debug("SET " + guiItems.getKey() + "|" + guiItem.toString() + " | meta: " + meta.toString());

            this.inventory.setItem(guiItems.getKey(), stack);
        }

        player.openInventory(this.inventory);
    }

    public void setItem(int slot, TebexGuiItem guiItem) {
        this.inventory.setItem(slot, guiItem.getStack());
    }

    public void updateTitle(String replace) {
        this.title = replace;
    }
}
