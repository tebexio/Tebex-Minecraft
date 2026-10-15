package io.tebex.plugin;

import io.tebex.plugin.command.TebexCommandExecutor;
import io.tebex.plugin.compat.ForgePermissionNodes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.fml.common.Mod;

/** Forge 1.21.1 loader entrypoint and complete lifecycle registration. */
@Mod(TebexForgePlugin.MOD_ID)
public final class TebexForgePlugin {
  public static final String MOD_ID = "tebex";
  private final ForgePlatform platform = new ForgePlatform();

  public TebexForgePlugin() {
    platform.runtime().loadConfig();
    MinecraftForge.EVENT_BUS.addListener(this::commands);
    MinecraftForge.EVENT_BUS.addListener(this::started);
    MinecraftForge.EVENT_BUS.addListener(this::stopping);
    MinecraftForge.EVENT_BUS.addListener(this::joined);
    MinecraftForge.EVENT_BUS.addListener(ForgePermissionNodes::register);
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
