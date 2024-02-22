package io.tebex.plugin.analytics.listener;

import com.google.common.collect.Maps;
import io.tebex.plugin.TebexPlugin;
import io.tebex.sdk.analytics.obj.AnalysePlayer;
import io.tebex.sdk.analytics.obj.PlayerType;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class JoinListener implements Listener {
    private final TebexPlugin platform;
    private final Map<UUID, String> joinMap;

    public JoinListener(TebexPlugin platform) {
        this.platform = platform;
        joinMap = Maps.newConcurrentMap();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onLogin(PlayerLoginEvent event) {
        Player bukkitPlayer = event.getPlayer();

        ServerPlatformConfig analyseConfig = platform.getPlatformConfig();
        if(analyseConfig.isPlayerExcluded(bukkitPlayer.getUniqueId())) {
            return;
        }

        joinMap.put(bukkitPlayer.getUniqueId(), event.getHostname());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player bukkitPlayer = event.getPlayer();

        ServerPlatformConfig analyseConfig = platform.getPlatformConfig();
        if(analyseConfig.isPlayerExcluded(bukkitPlayer.getUniqueId())) {
            platform.debug("Skipped tracking " + bukkitPlayer.getName() + " as they are an excluded player.");
            return;
        }

        AnalysePlayer player = new AnalysePlayer(
                bukkitPlayer.getName(),
                bukkitPlayer.getUniqueId(),
                bukkitPlayer.getAddress() != null ? bukkitPlayer.getAddress().getAddress().getHostAddress() : null
        );

        if(joinMap.containsKey(bukkitPlayer.getUniqueId())) {
            player.setDomain(joinMap.get(bukkitPlayer.getUniqueId()));
            joinMap.remove(bukkitPlayer.getUniqueId());
        }

        if(analyseConfig.shouldUseServerFirstJoinedAt()) {
            player.setFirstJoinedAt(new Date(bukkitPlayer.getFirstPlayed()));
        }

        // Bedrock Tracking
        if (! analyseConfig.isFloodgateHookEnabled()) {
            if (analyseConfig.getBedrockPrefix() != null && player.getName().startsWith(analyseConfig.getBedrockPrefix())) {
                player.setType(PlayerType.BEDROCK);
            }
        } else if (platform.getFloodgateHook() != null && platform.getFloodgateHook().isBedrock(event.getPlayer())) {
            player.setType(PlayerType.BEDROCK);
        }

        platform.debug("Tracking " + bukkitPlayer.getName() + " (" + player.getType() + ") that connected via: " + player.getDomain());

        platform.getAnalyticsSDK().getCountryFromIp(player.getIpAddress()).thenAccept(player::setCountry);

        platform.getAnalyticsManager().getPlayers().put(bukkitPlayer.getUniqueId(), player);
    }
}
