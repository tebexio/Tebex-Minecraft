package io.tebex.plugin.gui;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.util.ItemUtil;
import io.tebex.sdk.obj.Category;
import io.tebex.sdk.obj.CategoryPackage;
import io.tebex.sdk.obj.ICategory;
import io.tebex.sdk.obj.SubCategory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BuyGUI {
    private final TebexPlugin platform;
    private final YamlDocument config;

    public BuyGUI(TebexPlugin platform) {
        this.platform = platform;
        this.config = platform.getPlatformConfig().getYamlDocument();
    }

    private ScreenHandlerType<GenericContainerScreenHandler> getScreenHandlerType(final int rows) {
        ScreenHandlerType<GenericContainerScreenHandler> type;
        switch (rows) {
            case 1 -> type = ScreenHandlerType.GENERIC_9X1;
            case 2 -> type = ScreenHandlerType.GENERIC_9X2;
            case 3 -> type = ScreenHandlerType.GENERIC_9X3;
            case 4 -> type = ScreenHandlerType.GENERIC_9X4;
            case 5 -> type = ScreenHandlerType.GENERIC_9X5;
            default -> type = ScreenHandlerType.GENERIC_9X6;
        }
        return type;
    }

    private String convertToLegacyString(String str) {
        return str.replace("&", "§");
    }

    public void open(ServerPlayerEntity player) {
        List<Category> categories = platform.getStoreCategories();
        if (categories == null) {
            player.sendMessage(Text.of("Failed to get listing. Please contact an administrator."), false);
            platform.warning("Player " + player.getName() + " used buy command, but no listings are active in your store.","Ensure your store is set up and has at least one active listing. Use /tebex reload to load new listings.");
            return;
        }

        int rows = config.getInt("gui.menu.home.rows") < 1 ? categories.size() / 9 + 1 : config.getInt("gui.menu.home.rows");
        ListingGui listingGui = new ListingGui(rows, getScreenHandlerType(rows), player);
        listingGui.setTitle(Text.of(convertToLegacyString(config.getString("gui.menu.home.title", "Server Shop"))).getString());

        categories.sort(Comparator.comparingInt(Category::getOrder));

        categories.forEach(category -> listingGui.addItem(getCategoryItemBuilder(category).asGuiItem(action -> {
                    listingGui.close();
                    openCategoryMenu(player, category);
                }
        )));

        platform.executeBlocking(listingGui::open);
    }

    private void openCategoryMenu(ServerPlayerEntity player, ICategory category) {
        int rows = config.getInt("gui.menu.category.rows") < 1 ? category.getPackages().size() / 9 + 1 : config.getInt("gui.menu.category.rows");

        ListingGui subListingGui = new ListingGui(rows, getScreenHandlerType(rows), player);
        subListingGui.setTitle(Text.of(convertToLegacyString(config.getString("gui.menu.category.title").replace("%category%", category.getName()))).getString());

        category.getPackages().sort(Comparator.comparingInt(CategoryPackage::getOrder));

        if (category instanceof Category cat) {
            if (cat.getSubCategories() != null) {
                cat.getSubCategories().forEach(subCategory -> subListingGui.addItem(getCategoryItemBuilder(subCategory).asGuiItem(action -> {
                    openCategoryMenu(player, subCategory);
                })));

                TebexGuiItem backItem = getBackItemBuilder().asGuiItem(action -> {
                    action.setCancelled(true);
                    open(player);
                });
                int backItemSlot = subListingGui.getRows() * 9 - 5;
                subListingGui.addItem(backItemSlot, backItem);
                //subListingGui.setItem(backItemSlot, backItem);
            }
        } else if (category instanceof SubCategory) {
            SubCategory subCategory = (SubCategory) category;

            subListingGui.setTitle(Text.of(convertToLegacyString(config.getString("gui.menu.sub-category.title"))
                    .replace("%category%", subCategory.getParent().getName())
                    .replace("%sub_category%", category.getName())).getString());

            TebexGuiItem backItem = getBackItemBuilder().asGuiItem(action -> {
                action.setCancelled(true);
                openCategoryMenu(player, subCategory.getParent());
            });
            int backItemSlot = subListingGui.getRows() * 9 - 5;

            subListingGui.addItem(backItemSlot, backItem);
            //subListingGui.setItem(subListingGui.getRows() * 9 - 5,backItem);
        }

        category.getPackages().forEach(categoryPackage -> subListingGui.addItem(getPackageItemBuilder(categoryPackage).asGuiItem(action -> {
            player.closeHandledScreen();

            // Create Checkout Url
            platform.getSDK().createCheckoutUrl(categoryPackage.getId(), player.getName().getString()).thenAccept(checkout -> {
                player.sendMessage(Text.of("§aYou can checkout here: "), false);
                player.sendMessage(MutableText.of(new LiteralTextContent("§a"+checkout.getUrl())).setStyle(Style.EMPTY.withClickEvent(
                        new ClickEvent(ClickEvent.Action.OPEN_URL, checkout.getUrl()))), false);
            }).exceptionally(ex -> {
                player.sendMessage(Text.of("§cFailed to create checkout URL. Please contact an administrator."), false);
                platform.error("Failed to create checkout URL for a user.", ex);
                return null;
            });
        })));

        subListingGui.open();
    }

    private TebexItemBuilder getCategoryItemBuilder(ICategory category) {
        Section section = config.getSection("gui.item.category");

        String itemType = section.getString("material");

        Item defaultItem = ItemUtil.fromString(itemType).isPresent() ? ItemUtil.fromString(itemType).get() : null;
        Item item = ItemUtil.fromString(category.getGuiItem()).isPresent() ? ItemUtil.fromString(category.getGuiItem()).get() : defaultItem;

        String name = section.getString("name");
        List<String> lore = section.getStringList("lore");

        return TebexItemBuilder.from(item != null ? item : Items.BOOK)
                .hideFlags(ItemStack.TooltipSection.ENCHANTMENTS, ItemStack.TooltipSection.MODIFIERS, ItemStack.TooltipSection.UNBREAKABLE)
                .name(name != null ? remapLegacyFormatSeparator(italicize(handlePlaceholders(category, name))) : remapLegacyFormatSeparator(category.getName()))
                .lore(lore.stream().map(line ->  remapLegacyFormatSeparator(italicize(handlePlaceholders(category, line)))).collect(Collectors.toList()));
    }

    private TebexItemBuilder getPackageItemBuilder(CategoryPackage categoryPackage) {
        Section section = config.getSection("gui.item." + (categoryPackage.hasSale() ? "package-sale" : "package"));

        if (section == null) {
            platform.warning("Invalid configuration section for " + (categoryPackage.hasSale() ? "package-sale" : "package"), "Check that your package definition for `" + categoryPackage.getName() + "` in config.yml is valid.");
            return null;
        }

        String itemType = section.getString("material");
        Item material = Registries.ITEM.get(Identifier.tryParse(itemType.toLowerCase()));

        String name = section.getString("name");
        List<String> lore = section.getStringList("lore");


        MutableText guiName = MutableText.of(new LiteralTextContent(convertToLegacyString(name != null ? handlePlaceholders(categoryPackage, name) : categoryPackage.getName()))).setStyle(Style.EMPTY.withItalic(true));
        List<String> guiLore = lore.stream().map(line -> MutableText.of(new LiteralTextContent(convertToLegacyString(handlePlaceholders(categoryPackage, line)))).setStyle(Style.EMPTY.withItalic(true)).getString()).collect(Collectors.toList());

        TebexItemBuilder guiElementBuilder = TebexItemBuilder.from(material.asItem() != null ? material : Items.BOOK)
                .hideFlags(ItemStack.TooltipSection.ENCHANTMENTS, ItemStack.TooltipSection.UNBREAKABLE, ItemStack.TooltipSection.ADDITIONAL)
                .name(guiName.getString())
                .lore(guiLore);

        if (categoryPackage.hasSale()) {
            guiElementBuilder.enchant();
        }

        return guiElementBuilder;
    }

    private TebexItemBuilder getBackItemBuilder() {
        Section section = config.getSection("gui.item.back");

        String itemType = section.getString("material");
        Item material = Registries.ITEM.get(Identifier.tryParse(itemType.toLowerCase()));

        String name = section.getString("name");
        List<String> lore = section.getStringList("lore");

        return TebexItemBuilder.from(material.asItem() != null ? material : Items.BOOK)
                .hideFlags(ItemStack.TooltipSection.ENCHANTMENTS, ItemStack.TooltipSection.UNBREAKABLE, ItemStack.TooltipSection.ADDITIONAL)
                .name(Text.of(convertToLegacyString(name != null ? name : "§fBack")).getString())
                .lore(lore.stream().map(line -> ((MutableText)(Text.of(convertToLegacyString(line)))).setStyle(Style.EMPTY.withItalic(true)).getString()).collect(Collectors.toList()));
    }

    private String handlePlaceholders(Object obj, String str) {
        if (obj instanceof ICategory category) {
            str = str.replace("%category%", category.getName());
        } else if (obj instanceof CategoryPackage categoryPackage) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##");

            str = str
                    .replace("%package_name%", categoryPackage.getName())
                    .replace("%package_price%", decimalFormat.format(categoryPackage.getPrice()))
                    .replace("%package_currency_name%", platform.getStoreInformation().getStore().getCurrency().getIso4217())
                    .replace("%package_currency%", platform.getStoreInformation().getStore().getCurrency().getSymbol());

            if (categoryPackage.hasSale()) {
                str = str
                        .replace("%package_discount%", decimalFormat.format(categoryPackage.getSale().getDiscount()))
                        .replace("%package_sale_price%", decimalFormat.format(categoryPackage.getPrice() - categoryPackage.getSale().getDiscount()));
            }
        }

        return str;
    }

    private String italicize(String input) {
        return "§o" + input + "§r";
    }

    private String remapLegacyFormatSeparator(String input) {
        return input.replaceAll("&", "§");
    }
}
