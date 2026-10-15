package io.tebex.plugin;

import io.tebex.minecraft.platform.config.ServerPlatformConfig;
import io.tebex.minecraft.runtime.TebexRuntime;
import io.tebex.plugin.command.BuyCommand;
import io.tebex.plugin.command.TebexCommandExecutor;
import io.tebex.plugin.event.InventoryClickListener;
import io.tebex.plugin.event.PlayerJoinListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Properties;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/** Bukkit loader entrypoint and complete lifecycle registration. */
public final class TebexBukkitPlugin extends JavaPlugin {
  private BukkitPlatform platform;

  @Override
  public void onEnable() {
    platform = new BukkitPlatform(this);
    migrateConfig();

    TebexRuntime runtime = platform.runtime();
    runtime.loadConfig();
    runtime.start();

    TebexCommandExecutor tebexCommands = new TebexCommandExecutor(platform);
    PluginCommand pluginCommand = getCommand("tebex");
    if (pluginCommand == null) throw new IllegalStateException("Tebex command not found.");
    pluginCommand.setExecutor(tebexCommands);
    pluginCommand.setTabCompleter(tebexCommands);

    registerBuyCommandIfEnabled();
    registerEvents(new PlayerJoinListener(platform));
    registerEvents(new InventoryClickListener());
  }

  @Override
  public void onDisable() {
    if (platform != null) platform.runtime().shutdown();
  }

  private <T extends Listener> void registerEvents(T listener) {
    getServer().getPluginManager().registerEvents(listener, this);
  }

  private void migrateConfig() {
    if (new File(getDataFolder(), "config.yml").exists()) return;
    File oldConfig = new File("plugins/BuycraftX", "config.properties");
    if (!oldConfig.exists()) return;

    TebexRuntime runtime = platform.runtime();
    runtime.info("Detected legacy BuycraftX configuration. Attempting to migrate...");
    try {
      Properties properties = new Properties();
      try (java.io.InputStream input = Files.newInputStream(oldConfig.toPath())) {
        properties.load(input);
      }
      String secretKey = properties.getProperty("server-key");
      if (secretKey != null && !Objects.equals(secretKey, "INVALID")) {
        runtime.migrateLegacyConfig(properties);
        runtime.info("Successfully migrated your config from BuycraftX.");
      }
    } catch (IOException | RuntimeException error) {
      runtime.warning(
          "Failed to migrate BuycraftX configuration: " + error.getMessage(),
          "Please set your secret key with /tebex secret <key> to enable your store.");
    }
  }

  private void registerBuyCommandIfEnabled() {
    try {
      Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
      commandMapField.setAccessible(true);
      CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());
      ServerPlatformConfig config = platform.runtime().getConfig();
      if (config.isBuyCommandEnabled())
        commandMap.register(
            config.getBuyCommandName(), new BuyCommand(config.getBuyCommandName(), platform));
    } catch (Throwable error) {
      platform.runtime().error("Failed to register buy command: " + error.getMessage(), error);
    }
  }
}
