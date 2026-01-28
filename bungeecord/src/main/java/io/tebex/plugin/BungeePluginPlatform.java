package io.tebex.plugin;

import io.tebex.sdk.platform.BasePluginPlatform;
import io.tebex.sdk.platform.PlatformTelemetry;
import io.tebex.sdk.platform.PlatformType;
import io.tebex.sdk.util.CommandResult;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BungeePluginPlatform extends BasePluginPlatform {
    private final TebexBungeePlugin plugin;

    public BungeePluginPlatform(TebexBungeePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public PlatformType getType() {
        return PlatformType.BUNGEECORD;
    }

    @Override
    public File getRunningDirectory() {
        return plugin.getDataFolder();
    }

    @Override
    public boolean isOnlineMode() {
        return false;
    }

    @Override
    public CommandResult dispatchCommand(String command) {
        return CommandResult.from(plugin.getProxy().getPluginManager().dispatchCommand(plugin.getProxy().getConsole(), command));
    }

    @Override
    public void executeAsync(Runnable runnable) {
        plugin.getProxy().getScheduler().runAsync(plugin, runnable);
    }

    @Override
    public void executeAsyncLater(Runnable runnable, long time, TimeUnit unit) {
        plugin.getProxy().getScheduler().schedule(plugin, runnable, time, unit);
    }

    @Override
    public void executeBlocking(Runnable runnable) {
        // BungeeCord has no concept of "blocking"
        executeAsync(runnable);
    }

    @Override
    public void executeBlockingLater(Runnable runnable, long time, TimeUnit unit) {
        // BungeeCord has no concept of "blocking"
        executeAsyncLater(runnable, time, unit);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getPlayer(Object uuidOrUsername) {
        if(uuidOrUsername == null) return null;

        if (isOnlineMode() && !isGeyser() && uuidOrUsername instanceof UUID) {
            return (T) plugin.getProxy().getPlayer((UUID) uuidOrUsername);
        }

        return (T) plugin.getProxy().getPlayer((String) uuidOrUsername);
    }

    @Override
    public boolean isPlayerOnline(Object player) {
        return getPlayer(player) != null;
    }

    @Override
    public int getFreeSlots(Object player) {
        // Bungee has no concept of an inventory
        return 0;
    }

    @Override
    public String getPluginVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public void log(Level level, String message) {
        plugin.getLogger().log(level, message);
    }

    @Override
    public PlatformTelemetry getTelemetry() {
        String serverVersion = plugin.getProxy().getVersion();

        Pattern pattern = Pattern.compile("MC: (\\d+\\.\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(serverVersion);
        if (matcher.find()) {
            serverVersion = matcher.group(1);
        }

        return new PlatformTelemetry(
                getPluginVersion(),
                plugin.getProxy().getName(),
                serverVersion,
                System.getProperty("java.version"),
                System.getProperty("os.arch"),
                plugin.getProxy().getConfig().isOnlineMode()
        );
    }

    @Override
    public void sendPlayerMessage(String playerName, String message) {
        ProxiedPlayer player = getPlayer(playerName);
        if (player != null) {
            player.sendMessage(TextComponent.fromLegacyText(message));
        }
    }

    @Override
    public boolean hasPermission(String username, String permission) {
        ProxiedPlayer player = getPlayer(username);
        return player != null && player.hasPermission(permission);
    }

    public TebexBungeePlugin getPlugin() {
        return this.plugin;
    }
}
