package io.tebex.plugin.analytics.command.sub;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.sdk.analytics.SDK;
import io.tebex.sdk.exception.NotFoundException;
import io.tebex.sdk.platform.PlatformLang;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import net.minecraft.server.command.ServerCommandSource;

import java.io.IOException;

public class SecretCommand extends SubCommand {
    public SecretCommand(TebexPlugin platform) {
        super(platform, "secret", "tebex.secret");
    }

    @Override
    public void execute(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource sender = context.getSource();
        final TebexPlugin platform = getPlatform();

        String serverToken = context.getArgument("key", String.class);

        SDK sdk = platform.getAnalyticsSDK();
        ServerPlatformConfig platformConfig = platform.getPlatformConfig();
        YamlDocument configFile = platformConfig.getYamlDocument();

        sdk.setSecretKey(serverToken);

        platform.getAnalyticsSDK().getServerInformation().thenAccept(serverInformation -> {
            platformConfig.setAnalyticsSecretKey(serverToken);
            configFile.set("analytics.secret-key", serverToken);

            try {
                configFile.save();
            } catch (IOException e) {
                platform.sendMessage(sender, PlatformLang.ERROR_OCCURRED.get(e.getLocalizedMessage()));
            }

            platform.getAnalyticsSDK().completeServerSetup().thenAccept(v -> {
                if(sender.getEntity() != null) {
                    platform.sendMessage(sender, PlatformLang.SUCCESSFULLY_CONNECTED.get(serverInformation.getName()));
                }

                platform.getAnalyticsManager().init();
                platform.getAnalyticsManager().connect();
            }).exceptionally(ex -> {
                getPlatform().sendMessage(sender, PlatformLang.ERROR_OCCURRED.get(ex.getMessage()));
                ex.printStackTrace();
                return null;
            });
        }).exceptionally(ex -> {
            Throwable cause = ex.getCause();

            if(cause instanceof NotFoundException) {
                platform.sendMessage(sender, PlatformLang.INVALID_SECRET_KEY.get());
                platform.halt();
                return null;
            }

            platform.sendMessage(sender, PlatformLang.ERROR_OCCURRED.get(cause.getLocalizedMessage()));
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
}
