package io.tebex.plugin.analytics.listener;

import com.google.common.collect.Maps;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.mixin.HandshakeC2SPacketMixin;
import io.tebex.plugin.util.FabricEventHandler;
import io.tebex.sdk.analytics.obj.AnalysePlayer;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.NetworkState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stats;

import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class JoinListener implements ServerPlayConnectionEvents.Join {
    private final TebexPlugin platform;
    private final Map<UUID, String> joinMap;
    private final AtomicReference<String> joinAddress = new AtomicReference<>();

    public JoinListener(TebexPlugin platform) {
        this.platform = platform;
        joinMap = Maps.newConcurrentMap();

        ServerPlayConnectionEvents.JOIN.register(this);
        FabricEventHandler.ON_LOGIN.register((address, profile, reason) -> {
            if (reason != null) {
                return;
            }

            String joinAddress = this.joinAddress.getAndSet(null);
            if (joinAddress != null) {
                joinMap.put(profile.getId(), joinAddress);
            }
        });

        FabricEventHandler.ON_HANDSHAKE.register(packet -> {
            try {
                if (packet.getIntendedState() == NetworkState.LOGIN) {
                    // Get the value of the field
                    String address = ((HandshakeC2SPacketMixin) packet).getAddress();

                    if (address != null && address.contains("\u0000")) {
                        address = address.substring(0, address.indexOf('\u0000'));
                    }

                    joinAddress.set(address);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
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

        System.out.println("Joined from " + serverPlayer.getIp() + " with domain " + joinMap.get(serverPlayer.getUuid()));

        if(joinMap.containsKey(player.getUniqueId())) {
            player.setDomain(joinMap.get(player.getUniqueId()));
            joinMap.remove(player.getUniqueId());
        }

        if(analyseConfig.shouldUseServerFirstJoinedAt()) {
            int totalSeconds = serverPlayer.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(Stats.PLAY_ONE_MINUTE))/20;
            long totalTime = totalSeconds * 1000L; // Convert ticks to milliseconds
            Date firstJoined = new Date(System.currentTimeMillis() - totalTime);

            player.setFirstJoinedAt(firstJoined);
        }

        platform.debug("Tracking " + player.getName() + " (" + player.getType() + ") that connected via: " + player.getDomain());

        platform.getAnalyticsSDK().getCountryFromIp(player.getIpAddress()).thenAccept(player::setCountry);

        platform.getAnalyticsManager().getPlayers().put(player.getUniqueId(), player);
    }
}

