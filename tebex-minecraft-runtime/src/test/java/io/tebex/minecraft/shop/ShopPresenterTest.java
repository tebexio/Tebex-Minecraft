package io.tebex.minecraft.shop;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.Gson;
import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.minecraft.platform.config.ServerPlatformConfig;
import io.tebex.minecraft.runtime.MockPluginPlatform;
import io.tebex.model.Category;
import io.tebex.model.ServerInformation;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ShopPresenterTest {
  @TempDir Path directory;

  @Test
  void checkoutFailureDetailUnwrapsTheSdkCause() {
    CompletionException error =
        new CompletionException(new IllegalArgumentException("Package is unavailable"));

    assertEquals(
        "IllegalArgumentException: Package is unavailable",
        ShopPresenter.checkoutFailureDetail(error));
  }

  @Test
  void sharedNavigationUsesParentHistoryWithoutMutatingSdkModels() throws Exception {
    Host host = new Host(directory);
    try {
      Category parent =
          new Gson()
              .fromJson(
                  "{\"id\":1,\"name\":\"Ranks\",\"subcategories\":[{\"id\":2,\"name\":\"VIP\",\"packages\":[]}]}",
                  Category.class);
      host.setStoreCategories(Collections.singletonList(parent));
      AtomicReference<ShopPresenter.Menu> menu = new AtomicReference<>();
      new ShopPresenter(host.getRuntime()).open("Steve", menu::set, () -> {});
      menu.get().items.get(0).action.run();
      assertEquals("Viewing Ranks", menu.get().title);
      menu.get().items.get(0).action.run();
      assertEquals("Viewing VIP (Ranks)", menu.get().title);
      menu.get().items.get(menu.get().rows * 9 - 5).action.run();
      assertEquals("Viewing Ranks", menu.get().title);
      assertEquals(1, parent.getSubcategories().size());
    } finally {
      host.shutdown();
    }
  }

  @Test
  void overflowingCatalogueHasWorkingPaginationAndNoSlotCollision() throws Exception {
    Host host = new Host(directory);
    try {
      List<Category> categories = new ArrayList<>();
      for (int i = 0; i < 70; i++)
        categories.add(
            new Gson()
                .fromJson(
                    "{\"id\":" + i + ",\"order\":" + i + ",\"name\":\"Category " + i + "\"}",
                    Category.class));
      host.setStoreCategories(Collections.unmodifiableList(categories));
      AtomicReference<ShopPresenter.Menu> menu = new AtomicReference<>();
      new ShopPresenter(host.getRuntime()).open("Steve", menu::set, () -> {});
      Set<String> found = new HashSet<>();
      for (int page = 0; page < 10; page++) {
        ShopPresenter.Menu current = menu.get();
        current.items.forEach(
            (slot, item) -> {
              assertTrue(slot >= 0 && slot < current.rows * 9);
              if (item.name.startsWith("Category")) found.add(item.name);
            });
        ShopPresenter.Item next = current.items.get(current.rows * 9 - 1);
        if (next == null) break;
        next.action.run();
      }
      assertEquals(70, found.size());
    } finally {
      host.shutdown();
    }
  }

  static final class Host extends MockPluginPlatform {
    Host(Path directory) throws Exception {
      YamlDocument yaml = YamlDocument.create(directory.resolve("config.yml").toFile());
      yaml.set("gui.menu.home.rows", 3);
      yaml.set("gui.menu.category.title", "Viewing %category%");
      yaml.set("gui.menu.sub-category.title", "Viewing %sub_category% (%category%)");
      config = new ServerPlatformConfig(2);
      config.setYamlDocument(yaml);
      setConfig(config);
      setStoreInfo(
          new Gson()
              .fromJson(
                  "{\"account\":{\"currency\":{\"iso_4217\":\"USD\",\"symbol\":\"$\"}}}",
                  ServerInformation.class));
      setSetup(true);
    }
  }
}
