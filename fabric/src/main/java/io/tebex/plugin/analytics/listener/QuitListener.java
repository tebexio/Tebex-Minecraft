package io.tebex.plugin.analytics.listener;

import io.tebex.plugin.TebexPlugin;
import io.tebex.sdk.analytics.obj.AnalysePlayer;
import io.tebex.sdk.exception.NotFoundException;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.logging.Level;

public class QuitListener implements ServerPlayConnectionEvents.Disconnect {
    private final TebexPlugin platform;

    public QuitListener(TebexPlugin platform) {
        this.platform = platform;

        ServerPlayConnectionEvents.DISCONNECT.register(this);
    }

    @Override
    public void onPlayDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
        ServerPlayerEntity serverPlayer = handler.player;
        AnalysePlayer player = platform.getAnalyticsManager().getPlayer(serverPlayer.getUuid());
        if(player == null) return;

        platform.debug("Preparing to track " + player.getName() + "..");

        platform.getAnalyticsSDK().trackPlayerSession(player).thenAccept(successful -> {
            if(! successful) {
                platform.warning("Failed to track player session for " + player.getName() + ".");
                return;
            }

            platform.getAnalyticsManager().removePlayer(player.getUniqueId());
            platform.debug("Successfully tracked player session for " + player.getName() + ".");
        }).exceptionally(ex -> {
            Throwable cause = ex.getCause();
            platform.log(Level.WARNING, "Failed to track player session: " + cause.getMessage());

            if(cause instanceof NotFoundException) {
                platform.halt();
                return null;
            }

            cause.printStackTrace();
            return null;
        });
    }
}

