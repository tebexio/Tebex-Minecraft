package io.tebex.plugin;

import com.google.common.collect.Lists;
import io.tebex.plugin.command.TebexCommandExecutor;
import io.tebex.sdk.Tebex;
import io.tebex.sdk.obj.ServerEvent;
import io.tebex.sdk.util.Multithreading;
import io.tebex.sdk.util.TickScheduler;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class TebexFabricPlugin implements DedicatedServerModInitializer {
    private FabricPluginPlatform platform;

    public FabricPluginPlatform getPlatform() {
        return platform;
    }

    /**
     * Starts the Fabric platform.
     */
    @Override
    public void onInitializeServer() {
        platform = new FabricPluginPlatform(this);
        Tebex.init(platform);
        platform.loadPlatformConfig(); // loads the configuration file for the platform

        // Register event hooks
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            platform.setMinecraftServer(server);
            onEnableFabric();
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            Multithreading.shutdown();
            platform.getSDK().shutdown();
        });

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            fabricTick();
        });

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated, environment) -> {
            platform.debug("registering commands");
            new TebexCommandExecutor(platform).register(dispatcher);
        });
    }

    private void fabricTick() {
        List<Runnable> runnableTasks = TickScheduler.tick();
        for (Runnable runnable : runnableTasks) {
            try {
                getPlatform().getServer().execute(runnable);
            } catch (Throwable t) {
                getPlatform().error("Failed to execute runnable task: ", t);
            }
        }
    }

    private void onEnableFabric() {
        platform.initStore(); // uses loaded key to set current store and cache the available packages

        // Fabric specific registration
        platform.initBuyGui();
        new JoinListener(this);

        // Refresh store information every 5 minutes
        Multithreading.executeAsync(() -> {
            platform.refreshListings();
        }, 0, 5, TimeUnit.MINUTES);

        // Every 10 minutes clear the plugin event queue
        Multithreading.executeAsync(() -> {
            platform.getSDK().sendPluginEvents();
        }, 0, 10, TimeUnit.MINUTES);

        // Clear server events each minute
        Multithreading.executeAsync(() -> {
            List<ServerEvent> allServerEvents = platform.getJoinEvents();
            List<ServerEvent> runEvents;
            synchronized (allServerEvents) {
                runEvents = Lists.newArrayList(allServerEvents.subList(0, Math.min(allServerEvents.size(), 750)));
            }
            if (runEvents.isEmpty()) return;
            if (!platform.isSetup()) return;

            platform.getSDK().sendJoinEvents(runEvents)
                    .thenAccept(aVoid -> {
                        platform.clearSelectedPluginEvents(runEvents);
                        platform.debug("Successfully sent join events.");
                    })
                    .exceptionally(throwable -> {
                        platform.debug("Failed to send join events: " + throwable.getMessage());
                        return null;
                    });
        }, 0, 1, TimeUnit.MINUTES);
    }
}
