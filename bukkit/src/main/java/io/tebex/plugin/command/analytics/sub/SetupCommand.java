package io.tebex.plugin.command.analytics.sub;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.sdk.AnalyticsSDK;
import io.tebex.sdk.exception.NotFoundException;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import org.bukkit.command.CommandSender;

import java.io.IOException;

public class SetupCommand extends SubCommand {
    public SetupCommand(TebexPlugin platform) {
        super(platform, "setup", "analytics.setup");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String serverToken = args[0];
        TebexPlugin platform = getPlatform();

        AnalyticsSDK analyse = platform.getAnalyticsSDK();
        ServerPlatformConfig analyseConfig = platform.getPlatformConfig();
        YamlDocument configFile = analyseConfig.getYamlDocument();

        analyse.setServerToken(serverToken);

        platform.getAnalyticsSDK().getServerInformation().thenAccept(serverInformation -> {
            analyseConfig.setAnalyticsSecretKey(serverToken);
            configFile.set("analytics.secret-key", serverToken);

            try {
                configFile.save();
            } catch (IOException e) {
                getPlatform().sendMessage(sender, "&cFailed to setup the plugin. Check console for more information.");
                e.printStackTrace();
            }

            platform.getAnalyticsSDK().completeServerSetup().thenAccept(v -> {
                getPlatform().sendMessage(sender, "Connected to &b" + serverInformation.getName() + "&7.");
                platform.getAnalyticsManager().load();
                platform.getAnalyticsManager().connect();
            }).exceptionally(ex -> {
                getPlatform().sendMessage(sender, "&cFailed to setup the plugin. Check console for more information.");
                ex.printStackTrace();
                return null;
            });
        }).exceptionally(ex -> {
            Throwable cause = ex.getCause();

            if(cause instanceof NotFoundException) {
                getPlatform().sendMessage(sender, "&cNo server was found with the provided token. Please check the token and try again.");
                platform.halt();
                return null;
            }

            getPlatform().sendMessage(sender, "&cFailed to setup the plugin. Check console for more information.");
            cause.printStackTrace();
            return null;
        });
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public int getMinArgs() {
        return 1;
    }
}
