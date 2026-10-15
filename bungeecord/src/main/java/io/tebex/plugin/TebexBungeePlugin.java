package io.tebex.plugin;

import io.tebex.minecraft.runtime.TebexRuntime;
import io.tebex.plugin.command.TebexCommandExecutor;
import io.tebex.plugin.event.JoinListener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

/** BungeeCord loader entrypoint and complete lifecycle registration. */
public final class TebexBungeePlugin extends Plugin {
  private BungeePlatform platform;

  @Override
  public void onEnable() {
    platform = new BungeePlatform(this);
    TebexRuntime runtime = platform.runtime();
    runtime.loadConfig();
    runtime.start();
    runtime
        .getCommands()
        .setRestrictedToCommands("help", "forcecheck", "reload", "secret", "debug");
    TebexCommandExecutor tebexCommand = new TebexCommandExecutor(platform);
    PluginManager pluginManager = getProxy().getPluginManager();
    pluginManager.registerCommand(this, tebexCommand);

    getProxy().getPluginManager().registerListener(this, new JoinListener(platform));
  }

  @Override
  public void onDisable() {
    if (platform != null) platform.runtime().shutdown();
  }
}
