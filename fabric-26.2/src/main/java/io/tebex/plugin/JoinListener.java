package io.tebex.plugin;

import io.tebex.sdk.obj.QueuedPlayer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

public class JoinListener {
    private final TebexFabricPlugin plugin;

    public JoinListener(TebexFabricPlugin plugin) {
        this.plugin = plugin;
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onPlayerJoin(handler.player));
    }

    private void onPlayerJoin(ServerPlayer player) {
        Object playerId = plugin.getPlatform().getPlayerId(player.getName().getString(), player.getUUID());
        plugin.getPlatform().createJoinEvent(player.getUUID().toString(), player.getName().getString(), player.getIpAddress());

        if(! plugin.getPlatform().getQueuedPlayers().containsKey(playerId)) {
            return;
        }

        plugin.getPlatform().handleOnlineCommands(new QueuedPlayer(plugin.getPlatform().getQueuedPlayers().get(playerId), player.getName().getString(), player.getUUID().toString()));
    }
}
