package io.tebex.plugin;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.ProxyVersion;
import io.tebex.plugin.event.JoinListener;
import io.tebex.plugin.manager.CommandManager;
import io.tebex.sdk.Tebex;
import io.tebex.sdk.obj.ServerEvent;
import io.tebex.sdk.platform.BasePluginPlatform;
import io.tebex.sdk.platform.PlatformTelemetry;
import io.tebex.sdk.platform.PlatformType;
import io.tebex.sdk.util.CommandResult;
import net.kyori.adventure.text.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.slf4j.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Plugin(
        id = "tebex",
        name = "Tebex",
        version = Constants.VERSION,
        description = "The Velocity plugin for Tebex.",
        url = "https://tebex.io",
        authors = {"Tebex"}
)
public class TebexVelocityPlugin extends BasePluginPlatform {

    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;

    private CompletableFuture<?> commandChain = CompletableFuture.completedFuture(null);

    @Inject
    public TebexVelocityPlugin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    @Subscribe
    public void onEnable(ProxyInitializeEvent event) {
        Tebex.init(this);

        loadPlatformConfig(); // load config file for the platform

        initStore(); // use loaded key to set current store

        // Velocity specific
        new CommandManager(this).register();
        placeholderManager.registerDefaults();
        proxy.getEventManager().register(this, new JoinListener(this));

        // Clear server events every minute
        proxy.getScheduler()
                .buildTask(this, () -> {
                    List<ServerEvent> allServerEvents = getJoinEvents();
                    List<ServerEvent> runEvents;
                    synchronized (allServerEvents) {
                        runEvents = Lists.newArrayList(allServerEvents.subList(0, Math.min(allServerEvents.size(), 750)));
                    }
                    if (runEvents.isEmpty()) return;
                    if (!isSetup()) return;

                    getSDK().sendJoinEvents(runEvents)
                            .thenAccept(aVoid -> {
                                clearSelectedPluginEvents(runEvents);
                                debug("Successfully sent join events.");
                            })
                            .exceptionally(throwable -> {
                                debug("Failed to send join events: " + throwable.getMessage());
                                return null;
                            });
                })
                .repeat(1, TimeUnit.MINUTES)
                .schedule();

//        proxy.getScheduler()
//                .buildTask(this, () -> {
//                    getSDK().getServerInformation().thenAccept(information -> storeInformation = information);
//                    getSDK().getListing().thenAccept(listing -> storeCategories = listing);
//                })
//                .repeat(5, TimeUnit.MINUTES)
//                .delay(0, TimeUnit.MINUTES)
//                .schedule();
    }

    @Override
    public PlatformType getType() {
        return PlatformType.VELOCITY;
    }

    @Override
    public File getRunningDirectory() {
        return dataDirectory.toFile();
    }

    @Override
    public boolean isOnlineMode() {
        return proxy.getConfiguration().isOnlineMode();
    }

    @Override
    public synchronized CommandResult dispatchCommand(String command) {
        commandChain = commandChain
                .handle((ignored, throwable) -> null)
                .thenCompose(ignored ->
                        proxy.getCommandManager()
                                .executeAsync(proxy.getConsoleCommandSource(), command)
                                .whenComplete((result, throwable) -> {
                                    if (throwable != null) {
                                        error(
                                                String.format("Failed to execute Tebex command: %s", command),
                                                throwable
                                        );
                                    }
                                })
                );

        return CommandResult.from(true);
    }

    @Override
    public void executeAsync(Runnable runnable) {
        proxy.getScheduler()
                .buildTask(this, runnable)
                .schedule();
    }

    @Override
    public void executeAsyncLater(Runnable runnable, long time, TimeUnit unit) {
        proxy.getScheduler()
                .buildTask(this, runnable)
                .delay(time, unit)
                .schedule();
    }

    @Override
    public void executeBlocking(Runnable runnable) {
        // Velocity has no concept of "blocking"
        executeAsync(runnable);
    }

    @Override
    public void executeBlockingLater(Runnable runnable, long time, TimeUnit unit) {
        // Velocity has no concept of "blocking"
        executeAsyncLater(runnable, time, unit);
    }

    @Override
    public <T> T getPlayer(Object uuidOrUsername) {
        if(uuidOrUsername == null) return null;

        if (isOnlineMode() && !isGeyser() && uuidOrUsername instanceof UUID) {
            return (T)proxy.getPlayer(uuidOrUsername.toString());
        }

        return (T)proxy.getPlayer((String) uuidOrUsername);
    }

    @Override
    public int getFreeSlots(Object player) {
        // Bungee has no concept of an inventory
        return 0;
    }

    @Override
    public String getPluginVersion() {
        return Constants.VERSION;
    }

    @Override
    public void log(Level level, String message) {
        logger.atLevel(convertLevel(level)).log(message);
    }

    private org.slf4j.event.Level convertLevel(Level level){
        if(level == Level.SEVERE)
            return org.slf4j.event.Level.ERROR;
        else if(level == Level.WARNING)
            return org.slf4j.event.Level.WARN;
        else if(level == Level.INFO)
            return org.slf4j.event.Level.INFO;
        else if(level == Level.CONFIG || level == Level.FINE)
            return org.slf4j.event.Level.DEBUG;
        else
            return org.slf4j.event.Level.TRACE;
    }

    @Override
    public PlatformTelemetry getTelemetry() {
        ProxyVersion proxyVersion = proxy.getVersion();
        String serverVersion = proxyVersion.getVersion();

        Pattern pattern = Pattern.compile("MC: (\\d+\\.\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(serverVersion);
        if (matcher.find()) {
            serverVersion = matcher.group(1);
        }

        return new PlatformTelemetry(
                getPluginVersion(),
                proxyVersion.getName(),
                serverVersion,
                System.getProperty("java.version"),
                System.getProperty("os.arch"),
                proxy.getConfiguration().isOnlineMode()
        );
    }

    @Override
    public void sendPlayerMessage(String playerName, String message) {
        Player player = getPlayer(playerName);
        if (player != null) {
            player.sendMessage(Component.text(message));
        }
    }

    @Override
    public UUID getPlayerUniqueId(String playerName) {
        Player player = getPlayer(playerName);
        return player == null ? null : player.getUniqueId();
    }

    @Override
    public boolean hasPermission(String username, String permission) {
        Player player = getPlayer(username);
        return player != null && player.hasPermission(permission);
    }
}
