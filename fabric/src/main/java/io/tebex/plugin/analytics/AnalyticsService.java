package io.tebex.plugin.analytics;

import com.google.common.collect.Lists;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.analytics.listener.JoinListener;
import io.tebex.plugin.analytics.listener.QuitListener;
import io.tebex.plugin.analytics.manager.HeartbeatManager;
import io.tebex.plugin.obj.ServiceManager;
import io.tebex.plugin.util.Multithreading;
import io.tebex.sdk.analytics.SDK;
import io.tebex.sdk.analytics.obj.Event;
import io.tebex.sdk.exception.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AnalyticsService implements ServiceManager {
    private final TebexPlugin platform;
    private final HeartbeatManager heartbeatManager;
    private SDK sdk;
    private final List<Event> events;
    private boolean setup;

    public AnalyticsService(TebexPlugin platform) {
        this.platform = platform;
        this.heartbeatManager = new HeartbeatManager(platform);
        this.events = new ArrayList<>();
        this.sdk = new SDK(platform, platform.getPlatformConfig().getAnalyticsSecretKey());
    }

    @Override
    public void init() {
        // Register events.
        new JoinListener(platform);
        new QuitListener(platform);
    }

    @Override
    public void connect() {
        sdk.getServerInformation().thenAccept(serverInformation -> {
            platform.info(String.format("Connected to %s on Tebex Analytics.", serverInformation.getName()));

            this.setup = true;
            this.heartbeatManager.start();

            Multithreading.schedule(() -> {
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
            }, 0, 30, TimeUnit.SECONDS);
        }).exceptionally(ex -> {
            Throwable cause = ex.getCause();
            this.setup = false;
            this.heartbeatManager.stop();

            if (cause instanceof NotFoundException) {
                platform.warning("Failed to connect to Tebex Analytics. Please double-check your server key or run the setup command again.");
                platform.halt();
                return null;
            }

            platform.warning("Failed to get server information: " + cause.getMessage());
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
