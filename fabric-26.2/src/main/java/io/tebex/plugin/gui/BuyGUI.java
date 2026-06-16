package io.tebex.plugin.gui;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import io.tebex.plugin.FabricPluginPlatform;
import io.tebex.plugin.ItemUtil;
import io.tebex.sdk.obj.Category;
import io.tebex.sdk.obj.CategoryPackage;
import io.tebex.sdk.obj.ICategory;
import io.tebex.sdk.obj.SubCategory;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.net.URI;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BuyGUI {
    private final FabricPluginPlatform platform;
    private final YamlDocument config;

    public BuyGUI(FabricPluginPlatform platform) {
        this.platform = platform;
        this.config = platform.getPlatformConfig().getYamlDocument();
    }

    private MenuType<ChestMenu> getScreenHandlerType(final int rows) {
        MenuType<ChestMenu> type;
        switch (rows) {
            case 1 -> type = MenuType.GENERIC_9x1;
            case 2 -> type = MenuType.GENERIC_9x2;
            case 3 -> type = MenuType.GENERIC_9x3;
            case 4 -> type = MenuType.GENERIC_9x4;
            case 5 -> type = MenuType.GENERIC_9x5;
            default -> type = MenuType.GENERIC_9x6;
        }
        return type;
    }

    private String convertToLegacyString(String str) {
        return str.replace("&", "§");
    }

    public void open(ServerPlayer player) {
        List<Category> categories = platform.getStoreCategories();
        if (categories == null) {
            player.sendSystemMessage(Component.nullToEmpty("Failed to get listing. Please contact an administrator."), false);
            platform.warning("Player " + player.getName() + " used buy command, but no listings are active in your store.","Ensure your store is set up and has at least one active listing. Use /tebex reload to load new listings.");
            return;
        }

        int rows = config.getInt("gui.menu.home.rows") < 1 ? categories.size() / 9 + 1 : config.getInt("gui.menu.home.rows");
        ListingGui listingGui = new ListingGui(rows, getScreenHandlerType(rows), player);
        listingGui.setTitle(Component.nullToEmpty(convertToLegacyString(config.getString("gui.menu.home.title", "Server Shop"))).getString());

        categories.sort(Comparator.comparingInt(Category::getOrder));

        categories.forEach(category -> listingGui.addItem(getCategoryItemBuilder(category).asGuiItem(action -> {
                    listingGui.close();
                    openCategoryMenu(player, category);
                }
        )));

        platform.executeBlocking(listingGui::open);
    }

    private void openCategoryMenu(ServerPlayer player, ICategory category) {
        int rows = config.getInt("gui.menu.category.rows") < 1 ? category.getPackages().size() / 9 + 1 : config.getInt("gui.menu.category.rows");

        ListingGui subListingGui = new ListingGui(rows, getScreenHandlerType(rows), player);
        subListingGui.setTitle(Component.nullToEmpty(convertToLegacyString(config.getString("gui.menu.category.title").replace("%category%", category.getName()))).getString());

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

            subListingGui.setTitle(Component.nullToEmpty(convertToLegacyString(config.getString("gui.menu.sub-category.title"))
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
            player.closeContainer();

            // Create Checkout Url
            platform.getSDK().createCheckoutUrl(categoryPackage.getId(), player.getName().getString()).thenAccept(checkout -> {
                player.sendSystemMessage(Component.nullToEmpty("§aYou can checkout here: "), false);
                player.sendSystemMessage(MutableComponent.create(PlainTextContents.create("§a"+checkout.getUrl())).setStyle(Style.EMPTY.withClickEvent(
                        new ClickEvent.OpenUrl(URI.create(checkout.getUrl())))));
            }).exceptionally(ex -> {
                player.sendSystemMessage(Component.nullToEmpty("§cFailed to create checkout URL. Please contact an administrator."), false);
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
                .hideFlags(DataComponents.ENCHANTMENTS, DataComponents.ATTRIBUTE_MODIFIERS, DataComponents.UNBREAKABLE)
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
        Item material = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(itemType.toLowerCase()));

        String name = section.getString("name");
        List<String> lore = section.getStringList("lore");


        MutableComponent guiName = MutableComponent.create(PlainTextContents.create(convertToLegacyString(name != null ? handlePlaceholders(categoryPackage, name) : categoryPackage.getName()))).setStyle(Style.EMPTY.withItalic(true));
        List<String> guiLore = lore.stream().map(line -> MutableComponent.create(PlainTextContents.create(convertToLegacyString(handlePlaceholders(categoryPackage, line)))).setStyle(Style.EMPTY.withItalic(true)).getString()).collect(Collectors.toList());

        TebexItemBuilder guiElementBuilder = TebexItemBuilder.from(material.asItem() != null ? material : Items.BOOK)
                .hideFlags(DataComponents.ENCHANTMENTS, DataComponents.ATTRIBUTE_MODIFIERS, DataComponents.UNBREAKABLE)
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
        Item material = BuiltInRegistries.ITEM.getValue(Identifier.tryParse(itemType.toLowerCase()));

        String name = section.getString("name");
        List<String> lore = section.getStringList("lore");

        return TebexItemBuilder.from(material.asItem() != null ? material : Items.BOOK)
                .hideFlags(DataComponents.ENCHANTMENTS, DataComponents.ATTRIBUTE_MODIFIERS, DataComponents.UNBREAKABLE)
                .name(Component.nullToEmpty(convertToLegacyString(name != null ? name : "§fBack")).getString())
                .lore(lore.stream().map(line -> ((MutableComponent)(Component.nullToEmpty(convertToLegacyString(line)))).setStyle(Style.EMPTY.withItalic(true)).getString()).collect(Collectors.toList()));
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
