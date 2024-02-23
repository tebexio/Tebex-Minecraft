package io.tebex.plugin.analytics.command.sub;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.plugin.util.Lang;
import io.tebex.sdk.analytics.SDK;
import io.tebex.sdk.exception.NotFoundException;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import org.bukkit.command.CommandSender;

import java.io.IOException;

public class SecretCommand extends SubCommand {
    public SecretCommand(TebexPlugin platform) {
        super(platform, "secret", "analytics.setup");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        String serverToken = args[0];
        TebexPlugin platform = getPlatform();

        SDK analyticsSdk = platform.getAnalyticsSDK();
        ServerPlatformConfig analyseConfig = platform.getPlatformConfig();
        YamlDocument configFile = analyseConfig.getYamlDocument();

        analyticsSdk.setServerToken(serverToken);

        platform.getAnalyticsSDK().getServerInformation().thenAccept(serverInformation -> {
            analyseConfig.setAnalyticsSecretKey(serverToken);
            configFile.set("analytics.secret-key", serverToken);

            try {
                configFile.save();
            } catch (IOException e) {
                getPlatform().sendMessage(sender, Lang.ERROR_OCCURRED.get(e.getMessage()));
                e.printStackTrace();
            }

            platform.getAnalyticsSDK().completeServerSetup().thenAccept(v -> {
                platform.sendMessage(sender, Lang.CONNECT_SUCCESSFUL.get(serverInformation.getName()));
                platform.getAnalyticsManager().init();
                platform.getAnalyticsManager().connect();
            }).exceptionally(ex -> {
                getPlatform().sendMessage(sender, Lang.ERROR_OCCURRED.get(ex.getMessage()));
                ex.printStackTrace();
                return null;
            });
        }).exceptionally(ex -> {
            Throwable cause = ex.getCause();

            if(cause instanceof NotFoundException) {
                getPlatform().sendMessage(sender, Lang.INVALID_SECRET_KEY.get());
                platform.halt();
                return null;
            }

            getPlatform().sendMessage(sender, Lang.ERROR_OCCURRED.get(cause.getMessage()));
            cause.printStackTrace();
            return null;
        });
    }

    @Override
    public String getDescription() {
        return "Connects to your Tebex analytics.";
    }

    @Override
    public String getUsage() {
        return "<key>";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }
}
