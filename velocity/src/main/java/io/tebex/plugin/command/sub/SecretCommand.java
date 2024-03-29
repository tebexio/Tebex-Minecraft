package io.tebex.plugin.command.sub;

import com.velocitypowered.api.command.CommandSource;
import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.sdk.SDK;
import io.tebex.sdk.exception.ServerNotFoundException;
import io.tebex.sdk.platform.config.ProxyPlatformConfig;

import java.io.IOException;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

public class SecretCommand extends SubCommand {
    public SecretCommand(TebexPlugin platform) {
        super(platform, "secret", "tebex.setup");
    }

    @Override
    public void execute(CommandSource sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(legacySection().deserialize("§b[Tebex] §7Usage: §f/tebex secret <key>"));
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
                sender.sendMessage(legacySection().deserialize("§b[Tebex] §7Failed to save config: " + e.getMessage()));
            }

            sender.sendMessage(legacySection().deserialize("§b[Tebex] §7Connected to §b" + serverInformation.getServer().getName() + "§7."));
            platform.configure();
        }).exceptionally(ex -> {
            Throwable cause = ex.getCause();

            if (cause instanceof ServerNotFoundException) {
                sender.sendMessage(legacySection().deserialize("§b[Tebex] §7Server not found. Please check your secret key."));
                platform.halt();
            } else {
                sender.sendMessage(legacySection().deserialize("§b[Tebex] §cAn error occurred: " + cause.getMessage()));
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
