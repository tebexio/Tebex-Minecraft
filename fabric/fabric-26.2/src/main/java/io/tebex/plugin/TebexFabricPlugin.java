package io.tebex.plugin;

import io.tebex.plugin.command.TebexCommandExecutor;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

/** Fabric 26.2 loader entrypoint and complete lifecycle registration. */
public final class TebexFabricPlugin implements DedicatedServerModInitializer {
  @Override
  public void onInitializeServer() {
    FabricPlatform platform = new FabricPlatform();
    platform.runtime().loadConfig();
    ServerLifecycleEvents.SERVER_STARTED.register(
        server -> {
          platform.attachServer(server);
          platform.runtime().start();
        });
    ServerLifecycleEvents.SERVER_STOPPING.register(server -> platform.runtime().shutdown());
    ServerPlayConnectionEvents.JOIN.register(
        (handler, sender, server) -> {
          net.minecraft.server.level.ServerPlayer player = handler.player;
          platform
              .runtime()
              .onPlayerJoin(player.getName().getString(), player.getUUID(), player.getIpAddress());
        });
    CommandRegistrationCallback.EVENT.register(
        (dispatcher, dedicated, environment) ->
            new TebexCommandExecutor(platform.runtime()).register(dispatcher));
  }
}
