package io.tebex.plugin.analytics;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.analytics.listener.JoinListener;
import io.tebex.plugin.analytics.listener.QuitListener;
import io.tebex.plugin.analytics.manager.CommandManager;
import io.tebex.plugin.analytics.manager.HeartbeatManager;
import io.tebex.plugin.obj.ServiceManager;
import io.tebex.sdk.analytics.SDK;
import io.tebex.sdk.analytics.obj.AnalysePlayer;
import io.tebex.sdk.analytics.obj.Event;
import io.tebex.sdk.analytics.obj.PlayerEvent;
import io.tebex.sdk.exception.NotFoundException;
import io.tebex.sdk.store.obj.ServerEvent;
import org.bukkit.event.HandlerList;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public class AnalyticsService implements ServiceManager {
    private final TebexPlugin platform;
    private final HeartbeatManager heartbeatManager;
    private final JoinListener joinListener;
    private final QuitListener quitListener;

    private SDK sdk;
    private final List<Event> events;
    private boolean setup;

    public AnalyticsService(TebexPlugin platform) {
        this.platform = platform;
        this.heartbeatManager = new HeartbeatManager(platform);
        this.events = new ArrayList<>();
        sdk = new SDK(platform, platform.getPlatformConfig().getAnalyticsSecretKey());

        this.joinListener = new JoinListener(platform);
        this.quitListener = new QuitListener(platform);
    }

    @Override
    public void init() {
        new CommandManager(platform).register();

        // Unregister prior events.
        HandlerList.unregisterAll(joinListener);
        HandlerList.unregisterAll(quitListener);

        // Register events.
        platform.registerEvents(joinListener);
        platform.registerEvents(quitListener);
    }

    @Override
    public void connect() {
        sdk.getServerInformation().thenAccept(serverInformation -> {
            platform.info(String.format("Connected to %s on Tebex Analytics.", serverInformation.getName()));

            this.setup = true;
            this.heartbeatManager.start();

            platform.getAsyncScheduler().runAtFixedRate(() -> {
                ArrayList<Event> runEvents = Lists.newArrayList(events.subList(0, Math.min(events.size(), 750)));
                if (runEvents.isEmpty()) return;
                if (!setup) return;

                sdk.sendEvents(runEvents)
                        .thenAccept(aVoid -> {
                            events.removeAll(runEvents);
                            platform.debug("Successfully sent pending events.");
                        })
                        .exceptionally(throwable -> {
                            platform.debug("Failed to send pending events: " + throwable.getMessage());
                            return null;
                        });
            }, Duration.ZERO, Duration.ofSeconds(30));
        }).exceptionally(ex -> {
            Throwable cause = ex.getCause();
            this.setup = false;
            this.heartbeatManager.stop();

            if (cause instanceof NotFoundException) {
                platform.warning("Failed to connect to Tebex Analytics. Please double-check your server key or run the setup command again.");
                platform.halt();
                return null;
            }

            platform.warning("Failed to get analytics information: " + cause.getMessage());
            cause.printStackTrace();

            return null;
        });
    }

    public SDK getSdk() {
        return sdk;
    }

    @Override
    public boolean isSetup() {
        return setup;
    }

    @Override
    public void setSetup(boolean setup) {
        this.setup = setup;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void addEvent(Event event) {
        events.add(event);
    }
}
