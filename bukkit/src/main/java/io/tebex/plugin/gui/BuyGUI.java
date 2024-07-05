package io.tebex.plugin.gui;

import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.util.MaterialUtil;
import io.tebex.sdk.obj.Category;
import io.tebex.sdk.obj.CategoryPackage;
import io.tebex.sdk.obj.ICategory;
import io.tebex.sdk.obj.SubCategory;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class BuyGUI {
    private final TebexPlugin platform;
    private final FileConfiguration config;

    public BuyGUI(TebexPlugin platform) {
        this.platform = platform;
        this.config = platform.getConfig();
    }

    public void open(Player player) {
        List<Category> categories = platform.getStoreCategories();
        if (categories == null) {
            player.sendMessage(ChatColor.RED + "Failed to get listing. Please contact an administrator.");
            return;
        }

        ListingGui listingGui = new ListingGui()
                .title(config.getString("gui.menu.home.title", "Server Shop"))
                .rows(config.getInt("gui.menu.home.rows") < 1 ? categories.size() / 9 + 1 : config.getInt("gui.menu.home.rows"))
                .create();

        categories.sort(Comparator.comparingInt(Category::getOrder));

        categories.forEach(category -> {
            listingGui.addItem(getCategoryItemBuilder(category).asGuiItem(action -> {
                action.setCancelled(true);
                openCategoryMenu(player, category);
            }));
        });

        platform.executeBlocking(() -> listingGui.open(player));
    }

    private void openCategoryMenu(Player player, ICategory category) {
        ListingGui subListingGui = new ListingGui()
                .title(config.getString("gui.menu.category.title").replace("%category%", category.getName()))
                .rows(config.getInt("gui.menu.category.rows") < 1 ? category.getPackages().size() / 9 + 1 : config.getInt("gui.menu.category.rows"))
                .create();

        category.getPackages().sort(Comparator.comparingInt(CategoryPackage::getOrder));

        if(category instanceof Category) {
            Category cat = (Category) category;

            if(cat.getSubCategories() != null) {
                cat.getSubCategories().forEach(subCategory -> subListingGui.addItem(getCategoryItemBuilder(subCategory).asGuiItem(action -> {
                    action.setCancelled(true);
                    openCategoryMenu(player, subCategory);
                })));

                subListingGui.setItem(subListingGui.getRows() * 9 - 5, getBackItemBuilder().asGuiItem(action -> {
                    action.setCancelled(true);
                    open(player);
                }));
            }
        } else if(category instanceof SubCategory) {
            SubCategory subCategory = (SubCategory) category;

            subListingGui.updateTitle(config.getString("gui.menu.sub-category.title")
                    .replace("%category%", subCategory.getParent().getName())
                    .replace("%sub_category%", category.getName())
            );

            subListingGui.setItem(subListingGui.getRows() * 9 - 5, getBackItemBuilder().asGuiItem(action -> {
                action.setCancelled(true);
                openCategoryMenu(player, subCategory.getParent());
            }));
        }

        category.getPackages().forEach(categoryPackage -> subListingGui.addItem(getPackageItemBuilder(categoryPackage).asGuiItem(action -> {
            action.setCancelled(true);
            player.closeInventory();

            // Create Checkout Url
            platform.getSDK().createCheckoutUrl(categoryPackage.getId(), player.getName()).thenAccept(checkout -> {
                player.sendMessage(ChatColor.GREEN + "You can checkout here: " + checkout.getUrl());
            }).exceptionally(ex -> {
                player.sendMessage(ChatColor.RED + "Failed to create checkout URL. Please contact an administrator.");
                ex.printStackTrace();
                platform.sendTriageEvent(ex);
                return null;
            });
        })));

        platform.executeBlocking(() -> subListingGui.open(player));
    }

    private TebexItemBuilder getCategoryItemBuilder(ICategory category) {
        ConfigurationSection section = config.getConfigurationSection("gui.item.category");

        String itemType = section.getString("material");
        Material defaultMaterial = MaterialUtil.fromString(itemType).isPresent() ? MaterialUtil.fromString(itemType).get().parseMaterial() : null;
        Material material = MaterialUtil.fromString(category.getGuiItem()).isPresent() ? MaterialUtil.fromString(category.getGuiItem()).get().parseMaterial() : defaultMaterial;

        String name = section.getString("name");
        List<String> lore = section.getStringList("lore");

        return TebexItemBuilder.from(material != null ? material : Material.BOOK)
                .flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE)
                .name(name != null ? remapLegacyFormatSeparator(italicize(handlePlaceholders(category, name))) : remapLegacyFormatSeparator(category.getName()))
                .lore(lore.stream().map(line ->  remapLegacyFormatSeparator(italicize(handlePlaceholders(category, line)))).collect(Collectors.toList()));
    }

    private TebexItemBuilder getPackageItemBuilder(CategoryPackage categoryPackage) {
        ConfigurationSection section = config.getConfigurationSection("gui.item." + (categoryPackage.hasSale() ? "package-sale" : "package"));

        if(section == null) {
            platform.getLogger().warning("Invalid configuration section for " + (categoryPackage.hasSale() ? "package-sale" : "package"));
            return null;
        }

        String itemType = section.getString("material");

        Material defaultMaterial = MaterialUtil.fromString(itemType).isPresent() ? MaterialUtil.fromString(itemType).get().parseMaterial() : null;
        Material material = MaterialUtil.fromString(categoryPackage.getGuiItem()).isPresent() ? MaterialUtil.fromString(categoryPackage.getGuiItem()).get().parseMaterial() : defaultMaterial;

        String name = section.getString("name");
        List<String> lore = section.getStringList("lore");

        TebexItemBuilder itemBuilder = TebexItemBuilder.from(material != null ? material : Material.BOOK)
                .flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE)
                .name(name != null ? remapLegacyFormatSeparator(italicize(handlePlaceholders(categoryPackage, name))) : remapLegacyFormatSeparator(categoryPackage.getName()))
                .lore(lore.stream().map(line -> remapLegacyFormatSeparator(italicize(handlePlaceholders(categoryPackage, line)))).collect(Collectors.toList()));

        if(categoryPackage.hasSale()) {
            itemBuilder.enchant();
        }

        return itemBuilder;
    }

    private String italicize(String input) {
        return "§o" + input + "§r";
    }

    private String remapLegacyFormatSeparator(String input) {
        return input.replaceAll("&", "§");
    }

    private TebexItemBuilder getBackItemBuilder() {
        ConfigurationSection section = config.getConfigurationSection("gui.item.back");

        String itemType = section.getString("material");
        Material defaultMaterial = MaterialUtil.fromString(itemType).isPresent() ? MaterialUtil.fromString(itemType).get().parseMaterial() : null;
        Material material = MaterialUtil.fromString(section.getString("material")).isPresent() ? MaterialUtil.fromString(section.getString("material")).get().parseMaterial() : defaultMaterial;

        String name = section.getString("name");
        List<String> lore = section.getStringList("lore");

        return TebexItemBuilder.from(material != null ? material : Material.BOOK)
                .flags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE)
                .name(name != null ? remapLegacyFormatSeparator(italicize(name)) : "Back")
                .lore(lore.stream().map(line -> remapLegacyFormatSeparator(italicize(line))).collect(Collectors.toList()));
    }

    private String handlePlaceholders(Object obj, String str) {
        if(obj instanceof ICategory) {
            ICategory category = (ICategory) obj;

            str = str.replace("%category%", category.getName());
        } else if(obj instanceof CategoryPackage) {
            CategoryPackage categoryPackage = (CategoryPackage) obj;

            DecimalFormat decimalFormat = new DecimalFormat("#.##");

            str = str
                    .replace("%package_name%", categoryPackage.getName())
                    .replace("%package_price%", decimalFormat.format(categoryPackage.getPrice()))
                    .replace("%package_currency_name%", platform.getStoreInformation().getStore().getCurrency().getIso4217())
                    .replace("%package_currency%", platform.getStoreInformation().getStore().getCurrency().getSymbol());

            if(categoryPackage.hasSale()) {
                str = str
                        .replace("%package_discount%", decimalFormat.format(categoryPackage.getSale().getDiscount()))
                        .replace("%package_sale_price%", decimalFormat.format(categoryPackage.getPrice() - categoryPackage.getSale().getDiscount()));
            }
        }

        return str;
    }
}
