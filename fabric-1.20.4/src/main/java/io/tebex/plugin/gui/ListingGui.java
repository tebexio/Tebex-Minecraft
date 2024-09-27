package io.tebex.plugin.gui;

import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ListingGui {
    private Inventory inventory;

    private String title;
    private int rows;
    private ArrayList<String> lore;
    private HashMap<Integer, TebexGuiItem> guiItems;
    private final ScreenHandlerType<GenericContainerScreenHandler> containerScreenHandler;
    private final ServerPlayerEntity player;

    public ListingGui(int rows, ScreenHandlerType<GenericContainerScreenHandler> screenHandlerType, ServerPlayerEntity player) {
        this.rows = rows;
        this.inventory = new SimpleInventory(rows*9);
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
        this.inventory = new SimpleInventory(rows * 9);
        return this;
    }

    public Inventory getInventory() {
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
        this.inventory.clear();

        for (Map.Entry<Integer,TebexGuiItem> guiItems : guiItems.entrySet()) {
            TebexGuiItem guiItem = guiItems.getValue();
            ItemStack stack = guiItem.getStack();
            this.inventory.setStack(guiItems.getKey(), stack);
        }

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, playerEntity) -> new TebexBuyScreenHandler(containerScreenHandler, syncId, inv, inventory, rows, guiItems),
                Text.literal(title)
        ));
    }

    public void setItem(int slot, TebexGuiItem guiItem) {
        this.inventory.setStack(slot, guiItem.getStack());
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
