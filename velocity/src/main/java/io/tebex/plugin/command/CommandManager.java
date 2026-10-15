package io.tebex.plugin.command;

import com.velocitypowered.api.command.CommandMeta;
import io.tebex.plugin.VelocityPlatform;

/** Native registration only; command metadata and handlers live in tebex-minecraft-runtime. */
public final class CommandManager {
  private final VelocityPlatform platform;

  public CommandManager(VelocityPlatform platform) {
    this.platform = platform;
  }

  public void register() {
    platform.runtime().getCommands().useOptionalBanArguments();
    platform
        .runtime()
        .getCommands()
        .setRestrictedToCommands("secret", "reload", "forcecheck", "help", "ban", "debug");
    CommandMeta meta =
        platform
            .proxy()
            .getCommandManager()
            .metaBuilder("tebex")
            .aliases("tbx", "buycraft")
            .plugin(platform.plugin())
            .build();
    platform.proxy().getCommandManager().register(meta, new TebexCommand(this));
  }

  public VelocityPlatform getPlatform() {
    return platform;
  }
}
