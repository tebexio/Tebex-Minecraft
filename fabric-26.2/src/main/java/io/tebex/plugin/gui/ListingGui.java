package io.tebex.plugin.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

public class ListingGui {
    private Container inventory;

    private String title;
    private int rows;
    private ArrayList<String> lore;
    private HashMap<Integer, TebexGuiItem> guiItems;
    private final MenuType<ChestMenu> containerScreenHandler;
    private final ServerPlayer player;

    public ListingGui(int rows, MenuType<ChestMenu> screenHandlerType, ServerPlayer player) {
        this.rows = rows;
        this.inventory = new SimpleContainer(rows*9);
        this.lore = new ArrayList<>();
        this.guiItems = new HashMap<>();
        this.containerScreenHandler = screenHandlerType;
        this.player = player;
    }

    public ListingGui setTitle(String title) {
        this.title = title;
        return this;
    }

    public ListingGui lore(ArrayList<String> lore) {
        this.lore = lore;
        return this;
    }

    public ListingGui create() {
        this.inventory = new SimpleContainer(rows * 9);
        return this;
    }

    public Container getInventory() {
        return inventory;
    }

    public int getRows() {
        return this.rows;
    }

    public void addItem(TebexGuiItem guiItem) {
        int nextSlot = 0;
        while (guiItems.containsKey(nextSlot) && nextSlot < rows * 9) {
            nextSlot++;
        }
        this.guiItems.put(nextSlot, guiItem);
    }

    public void addItem(int index, TebexGuiItem guiItem) {
        this.guiItems.put(index, guiItem);
    }

    public void open()
    {
        this.inventory.clearContent();

        for (Map.Entry<Integer,TebexGuiItem> guiItems : guiItems.entrySet()) {
            TebexGuiItem guiItem = guiItems.getValue();
            ItemStack stack = guiItem.getStack();
            this.inventory.setItem(guiItems.getKey(), stack);
        }

        player.openMenu(new SimpleMenuProvider(
                (syncId, inv, playerEntity) -> new TebexBuyScreenHandler(containerScreenHandler, syncId, inv, inventory, rows, guiItems),
                Component.literal(title)
        ));
    }

    public void setItem(int slot, TebexGuiItem guiItem) {
        this.inventory.setItem(slot, guiItem.getStack());
    }

    public void updateTitle(String replace) {
        this.title = replace;
    }

    public TebexGuiItem getItemInSlot(int slot) {
        return this.guiItems.get(slot);
    }

    public void close() {
    }
}
