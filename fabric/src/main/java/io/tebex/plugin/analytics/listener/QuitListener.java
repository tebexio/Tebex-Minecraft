package io.tebex.plugin.analytics.listener;

import io.tebex.plugin.TebexPlugin;
import io.tebex.sdk.analytics.obj.Event;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Date;

public class QuitListener implements ServerPlayConnectionEvents.Disconnect {
    private final TebexPlugin platform;

    public QuitListener(TebexPlugin platform) {
        this.platform = platform;

        ServerPlayConnectionEvents.DISCONNECT.register(this);
    }

    @Override
    public void onPlayDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
        ServerPlayerEntity serverPlayer = handler.player;

        Event playerEvent = new Event("player:quit", "Analyse", new Date(), serverPlayer.getUuid());
        platform.getAnalyticsManager().getEvents().add(playerEvent);

        platform.debug("Preparing to track " + serverPlayer.getName() + "..");
    }
}

