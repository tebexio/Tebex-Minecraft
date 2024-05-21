package io.tebex.plugin.store.placeholder;

import io.tebex.sdk.store.obj.QueuedPlayer;
import io.tebex.sdk.store.placeholder.Placeholder;
import io.tebex.sdk.store.placeholder.PlaceholderManager;
import io.tebex.sdk.util.UUIDUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

public class BukkitNamePlaceholder implements Placeholder {
    private final PlaceholderManager placeholderManager;

    public BukkitNamePlaceholder(PlaceholderManager placeholderManager) {
        this.placeholderManager = placeholderManager;
    }

    @Override
    public String handle(QueuedPlayer player, String command) {
        if (player.getUuid() == null || player.getUuid().isEmpty()) {
            return placeholderManager.getUsernameRegex().matcher(command).replaceAll(player.getName());
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUIDUtil.mojangIdToJavaId(player.getUuid()));
        if (offlinePlayer == null || !offlinePlayer.hasPlayedBefore()) {
            return placeholderManager.getUsernameRegex().matcher(command).replaceAll(player.getName());
        }

        return placeholderManager.getUsernameRegex().matcher(command).replaceAll(offlinePlayer.getName());
    }
}