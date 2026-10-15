package io.tebex.plugin;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import io.tebex.plugin.command.CommandManager;
import io.tebex.plugin.event.JoinListener;
import java.nio.file.Path;
import org.slf4j.Logger;

@Plugin(
    id = "tebex",
    name = "Tebex",
    version = Constants.VERSION,
    description = "The Velocity plugin for Tebex.",
    url = "https://tebex.io",
    authors = {"Tebex"})
/** Velocity loader entrypoint and complete lifecycle registration. */
public final class TebexVelocityPlugin {
  private final VelocityPlatform platform;

  @Inject
  public TebexVelocityPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
    this.platform = new VelocityPlatform(this, proxy, logger, dataDirectory);
  }

  @Subscribe
  public void onEnable(ProxyInitializeEvent event) {

    platform.runtime().loadConfig();
    platform.runtime().start();
    new CommandManager(platform).register();
    platform.proxy().getEventManager().register(this, new JoinListener(platform));
  }

  @Subscribe
  public void onDisable(ProxyShutdownEvent event) {
    platform.runtime().shutdown();
  }
}
