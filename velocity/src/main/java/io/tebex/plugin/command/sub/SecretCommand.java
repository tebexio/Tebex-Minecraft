package io.tebex.plugin.command.sub;

import com.velocitypowered.api.command.CommandSource;
import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.sdk.SDK;
import io.tebex.sdk.exception.ServerNotFoundException;
import io.tebex.sdk.platform.config.ProxyPlatformConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.IOException;

public class SecretCommand extends SubCommand {
    public SecretCommand(TebexPlugin platform) {
        super(platform, "secret", "tebex.setup");
    }

    @Override
    public void execute(CommandSource sender, String[] args) {
        if(args.length == 0) {
            sender.sendMessage(Component.text("[Tebex] ").color(NamedTextColor.AQUA).append(Component.text("Usage: ").color(NamedTextColor.GRAY)).append(Component.text("/tebex secret <key>").color(NamedTextColor.WHITE)));
            return;
        }

        String serverToken = args[0];
        TebexPlugin platform = getPlatform();

        SDK analyse = platform.getSDK();
        ProxyPlatformConfig analyseConfig = platform.getPlatformConfig();
        YamlDocument configFile = analyseConfig.getYamlDocument();

        analyse.setSecretKey(serverToken);

        platform.getSDK().getServerInformation().thenAccept(serverInformation -> {
            analyseConfig.setSecretKey(serverToken);
            configFile.set("server.secret-key", serverToken);

            try {
                configFile.save();
            } catch (IOException e) {
                sender.sendMessage(Component.text("[Tebex] ").color(NamedTextColor.AQUA).append(Component.text("Failed to save config: " + e.getMessage()).color(NamedTextColor.GRAY)));
            }

            sender.sendMessage(Component.text("[Tebex] ").color(NamedTextColor.AQUA)
                    .append(Component.text("Connected to ").color(NamedTextColor.GRAY))
                    .append(Component.text(serverInformation.getServer().getName()).color(NamedTextColor.AQUA))
                    .append(Component.text(".").color(NamedTextColor.GRAY)));
            platform.configure();
        }).exceptionally(ex -> {
            Throwable cause = ex.getCause();

            if(cause instanceof ServerNotFoundException) {
                sender.sendMessage(Component.text("[Tebex] ").color(NamedTextColor.AQUA).append(Component.text("Server not found. Please check your secret key.").color(NamedTextColor.GRAY)));
                platform.halt();
            } else {
                sender.sendMessage(Component.text("[Tebex] ").color(NamedTextColor.AQUA).append(Component.text("An error occurred: " + cause.getMessage()).color(NamedTextColor.RED)));
                cause.printStackTrace();
            }

            return null;
        });
    }

    @Override
    public String getDescription() {
        return "Connects to your Tebex store.";
    }

    @Override
    public String getUsage() {
        return "<key>";
    }
}
