package io.tebex.plugin;

import io.tebex.plugin.command.TebexCommandExecutor;
import io.tebex.plugin.compat.NeoForgePermissionNodes;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/** NeoForge 26.2 loader entrypoint and complete lifecycle registration. */
@Mod(TebexNeoForgePlugin.MOD_ID)
public final class TebexNeoForgePlugin {
  public static final String MOD_ID = "tebex";
  private final NeoForgePlatform platform = new NeoForgePlatform();

  public TebexNeoForgePlugin(IEventBus modEventBus) {
    platform.runtime().loadConfig();
    NeoForge.EVENT_BUS.addListener(this::started);
    NeoForge.EVENT_BUS.addListener(this::stopping);
    NeoForge.EVENT_BUS.addListener(this::commands);
    NeoForge.EVENT_BUS.addListener(this::joined);
    NeoForge.EVENT_BUS.addListener(NeoForgePermissionNodes::register);
  }

  private void started(ServerStartedEvent event) {
    platform.attachServer(event.getServer());
    platform.runtime().start();
  }

  private void stopping(ServerStoppingEvent event) {
    platform.runtime().shutdown();
  }

  private void commands(RegisterCommandsEvent event) {
    new TebexCommandExecutor(platform.runtime()).register(event.getDispatcher());
  }

  private void joined(PlayerEvent.PlayerLoggedInEvent event) {
    if (event.getEntity() instanceof ServerPlayer) {
      ServerPlayer player = (ServerPlayer) event.getEntity();
      platform
          .runtime()
          .onPlayerJoin(player.getName().getString(), player.getUUID(), player.getIpAddress());
    }
  }
}
