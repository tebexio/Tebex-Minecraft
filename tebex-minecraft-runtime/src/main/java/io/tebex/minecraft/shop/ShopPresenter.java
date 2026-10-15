package io.tebex.minecraft.shop;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.minecraft.runtime.SdkSession;
import io.tebex.minecraft.runtime.TebexRuntime;
import io.tebex.model.Category;
import io.tebex.model.CategoryPackage;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Consumer;

/**
 * Shared shop navigation, configured presentation and checkout actions; contains no native UI
 * types.
 */
public final class ShopPresenter {
  private final TebexRuntime platform;

  public ShopPresenter(TebexRuntime platform) {
    this.platform = platform;
  }

  public void open(String player, Consumer<Menu> render, Runnable close) {
    new Session(player, render, close).show(null, Collections.emptyList(), 0);
  }

  private final class Session {
    final String player;
    final Consumer<Menu> render;
    final Runnable close;
    final SdkSession connection = platform.getSession();

    Session(String player, Consumer<Menu> render, Runnable close) {
      this.player = player;
      this.render = render;
      this.close = close;
    }

    void show(Category category, List<Category> parents, int page) {
      if (!platform.isSetup() || connection != platform.getSession()) {
        close.run();
        return;
      }
      YamlDocument config = platform.getConfig().getYamlDocument();
      String kind = category == null ? "home" : parents.isEmpty() ? "category" : "sub-category";
      String title = config.getString("gui.menu." + kind + ".title", "Server Shop");
      if (category != null)
        title =
            title
                .replace(
                    "%category%",
                    parents.isEmpty()
                        ? category.getName()
                        : parents.get(parents.size() - 1).getName())
                .replace("%sub_category%", category.getName());
      List<Item> content = new ArrayList<>();
      List<Category> categories =
          new ArrayList<>(
              category == null ? platform.getStoreCategories() : category.getSubcategories());
      categories.sort(Comparator.comparingInt(Category::getOrder));
      for (Category child : categories) {
        List<Category> nextParents = new ArrayList<>(parents);
        if (category != null) nextParents.add(category);
        content.add(
            item("category", child.getGuiItem(), child, null, () -> show(child, nextParents, 0)));
      }
      if (category != null && !category.isOnlySubcategories()) {
        List<CategoryPackage> packages = new ArrayList<>(category.getPackages());
        packages.sort(Comparator.comparingInt(CategoryPackage::getOrder));
        for (CategoryPackage product : packages)
          content.add(
              item(
                  onSale(product) ? "package-sale" : "package",
                  product.getGuiItem(),
                  category,
                  product,
                  () -> {
                    close.run();
                    if (connection != platform.getSession() || connection.isClosed()) return;
                    connection
                        .request(
                            (api, secret) -> api.createCheckoutUrl(secret, product.getId(), player))
                        .thenAccept(url -> platform.sendCheckoutLink(player, url.getUrl()))
                        .exceptionally(
                            error -> {
                              platform.debug(
                                  "Checkout URL request failed: " + checkoutFailureDetail(error));
                              platform.callPlayer(
                                  player,
                                  () -> {
                                    if (connection == platform.getSession()
                                        && !connection.isClosed())
                                      platform.sendPlayerMessage(
                                          player,
                                          "Failed to create checkout URL. Please contact an administrator.");
                                    return null;
                                  });
                              return null;
                            });
                  }));
      }
      int rows = config.getInt("gui.menu." + kind + ".rows", 3);
      if (rows < 1) rows = (content.size() + 3 + 8) / 9;
      rows = Math.max(1, Math.min(6, rows));
      int size = rows * 9, back = size - 5, previous = size - 9, next = size - 1;
      int capacity = size - 3;
      int safePage = Math.max(0, Math.min(page, Math.max(0, (content.size() - 1) / capacity)));
      Map<Integer, Item> slots = new LinkedHashMap<>();
      int cursor = safePage * capacity;
      for (int slot = 0; slot < size && cursor < content.size(); slot++) {
        if (slot != back && slot != previous && slot != next)
          slots.put(slot, content.get(cursor++));
      }
      if (category != null)
        slots.put(
            back,
            item(
                "back",
                null,
                category,
                null,
                () -> {
                  if (parents.isEmpty()) show(null, Collections.emptyList(), 0);
                  else
                    show(
                        parents.get(parents.size() - 1),
                        new ArrayList<>(parents.subList(0, parents.size() - 1)),
                        0);
                }));
      if (safePage > 0)
        slots.put(
            previous,
            new Item(
                "ARROW",
                "ARROW",
                "Previous page",
                Collections.emptyList(),
                false,
                () -> show(category, parents, safePage - 1)));
      if (cursor < content.size())
        slots.put(
            next,
            new Item(
                "ARROW",
                "ARROW",
                "Next page",
                Collections.emptyList(),
                false,
                () -> show(category, parents, safePage + 1)));
      render.accept(new Menu(legacy(title), rows, slots));
    }

    Item item(
        String kind, String material, Category category, CategoryPackage product, Runnable action) {
      YamlDocument config = platform.getConfig().getYamlDocument();
      String prefix = "gui.item." + kind + ".";
      String fallback = config.getString(prefix + "material", "BOOK");
      String name =
          config.getString(
              prefix + "name",
              product != null ? product.getName() : category != null ? category.getName() : "Back");
      List<String> lore = new ArrayList<>();
      for (String line : config.getStringList(prefix + "lore"))
        lore.add(format(line, category, product));
      return new Item(
          material,
          fallback,
          format(name, category, product),
          lore,
          product != null && onSale(product),
          action);
    }
  }

  static String checkoutFailureDetail(Throwable error) {
    Throwable cause = error;
    while (cause.getCause() != null && cause.getCause() != cause) cause = cause.getCause();
    String message = cause.getMessage();
    return cause.getClass().getSimpleName()
        + (message == null || message.trim().isEmpty() ? "" : ": " + message);
  }

  private static boolean onSale(CategoryPackage product) {
    return product.getSale() != null && product.getSale().isActive();
  }

  private String format(String text, Category category, CategoryPackage product) {
    if (category != null)
      text =
          text.replace("%category%", category.getName())
              .replace("%sub_category%", category.getName());
    if (product != null) {
      DecimalFormat number = new DecimalFormat("#.##");
      text =
          text.replace("%package_name%", product.getName())
              .replace("%package_price%", number.format(product.getPrice()))
              .replace("%package_sale_price%", number.format(product.getEffectivePrice()))
              .replace(
                  "%package_discount%",
                  number.format(product.getSale() == null ? 0 : product.getSale().getDiscount()))
              .replace("%package_currency_name%", platform.getAccount().getCurrency().getIso4217())
              .replace("%package_currency%", platform.getAccount().getCurrency().getSymbol());
    }
    return legacy(text);
  }

  private static String legacy(String text) {
    return text.replace('&', '\u00a7');
  }

  public static final class Menu {
    public final String title;
    public final int rows;
    public final Map<Integer, Item> items;

    Menu(String title, int rows, Map<Integer, Item> items) {
      this.title = title;
      this.rows = rows;
      this.items = Collections.unmodifiableMap(items);
    }
  }

  public static final class Item {
    public final String material, fallback, name;
    public final List<String> lore;
    public final boolean enchanted;
    public final Runnable action;

    Item(
        String material,
        String fallback,
        String name,
        List<String> lore,
        boolean enchanted,
        Runnable action) {
      this.material = material;
      this.fallback = fallback;
      this.name = name;
      this.lore = Collections.unmodifiableList(lore);
      this.enchanted = enchanted;
      this.action = action;
    }
  }
}
