package io.tebex.plugin.analytics.service;

import io.tebex.sdk.platform.service.PlayerCountService;
import org.bukkit.Bukkit;

public class BukkitPlayerCountService implements PlayerCountService {
    @Override
    public int getPlayerCount() {
        return Bukkit.getOnlinePlayers().size();
    }
}
