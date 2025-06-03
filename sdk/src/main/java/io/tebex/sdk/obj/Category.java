package io.tebex.sdk.obj;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Category implements ICategory {
    private final int id;
    private final int order;
    private final String name;
    private final String guiItem;
    private final boolean onlySubcategories;
    private List<SubCategory> subCategories;
    private final List<CategoryPackage> categoryPackages;
    private final boolean tiered;

    /**
     * If this is a tiered category and the usernameId is provided, this will be the active tier information for this category.
     */
    private final Tier activeTier;

    public Category(int id, int order, String name, String guiItem, boolean onlySubcategories, List<CategoryPackage> categoryPackages, boolean tiered, Tier activeTier) {
        this.id = id;
        this.order = order;
        this.name = name;
        this.guiItem = guiItem;
        this.onlySubcategories = onlySubcategories;
        this.subCategories = new ArrayList<>();
        this.categoryPackages = categoryPackages;
        this.tiered = tiered;
        this.activeTier = activeTier;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getGuiItem() {
        return guiItem;
    }

    public boolean isOnlySubcategories() {
        return onlySubcategories;
    }

    public List<SubCategory> getSubCategories() {
        return subCategories;
    }

    public List<CategoryPackage> getPackages() {
        return categoryPackages;
    }

    public void setSubCategories(List<SubCategory> subCategories) {
        this.subCategories = subCategories;
    }

    public static Category fromJsonObject(JsonObject jsonObject) {
        Category category = new Category(
                jsonObject.get("id").getAsInt(),
                jsonObject.get("order").getAsInt(),
                jsonObject.get("name").getAsString(),
                jsonObject.get("gui_item").getAsString(),
                jsonObject.has("only_subcategories") && jsonObject.get("only_subcategories").getAsBoolean(),
                jsonObject.getAsJsonArray("packages").asList().stream().map(item -> CategoryPackage.fromJsonObject(item.getAsJsonObject())).collect(Collectors.toList()),
                jsonObject.get("tiered").getAsBoolean(),
                Tier.fromJsonObject(jsonObject.get("active_tier").getAsJsonObject())
        );

        category.setSubCategories(jsonObject.getAsJsonArray("subcategories").asList().stream().map(item -> SubCategory.fromJsonObject(item.getAsJsonObject(), category)).collect(Collectors.toList()));

        return category;
    }

    @Override
    public String toString() {
        return "Category{" +
                "id=" + id +
                ", order=" + order +
                ", name='" + name + '\'' +
                ", onlySubcategories=" + onlySubcategories +
                ", subCategories=" + subCategories +
                ", packages=" + categoryPackages +
                '}';
    }
}
