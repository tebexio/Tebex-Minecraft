package io.tebex.plugin.analytics.listener;

import com.google.common.collect.Maps;
import io.tebex.plugin.TebexPlugin;
import io.tebex.sdk.analytics.obj.AnalysePlayer;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.UUID;

public class JoinListener implements ServerPlayConnectionEvents.Join {
    private final TebexPlugin platform;
    private final Map<UUID, String> joinMap;

    public JoinListener(TebexPlugin platform) {
        this.platform = platform;
        joinMap = Maps.newConcurrentMap();

        ServerPlayConnectionEvents.JOIN.register(this);
    }

    @Override
    public void onPlayReady(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        ServerPlayerEntity serverPlayer = handler.player;

        ServerPlatformConfig analyseConfig = platform.getPlatformConfig();
        if(analyseConfig.isPlayerExcluded(serverPlayer.getUuid())) {
            platform.debug("Skipped tracking " + serverPlayer.getName() + " as they are an excluded serverPlayer.");
            return;
        }

        AnalysePlayer player = new AnalysePlayer(
                serverPlayer.getName().asString(),
                serverPlayer.getUuid(),
                serverPlayer.getIp()
        );

        if(joinMap.containsKey(player.getUniqueId())) {
            player.setDomain(joinMap.get(player.getUniqueId()));
            joinMap.remove(player.getUniqueId());
        }

        if(analyseConfig.shouldUseServerFirstJoinedAt()) {
            // TODO: Find a way to get the first joined date
//            player.setFirstJoinedAt(new Date(player.getFirstPlayed()));
        }

        platform.debug("Tracking " + player.getName() + " (" + player.getType() + ") that connected via: " + player.getDomain());

        platform.getAnalyticsSDK().getCountryFromIp(player.getIpAddress()).thenAccept(player::setCountry);

        platform.getAnalyticsManager().getPlayers().put(player.getUniqueId(), player);
    }
}

