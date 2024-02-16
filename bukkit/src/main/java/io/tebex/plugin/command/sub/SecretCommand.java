package io.tebex.plugin.command.sub;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.Lang;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.plugin.gui.BuyGUI;
import io.tebex.sdk.StoreSDK;
import io.tebex.sdk.exception.NotFoundException;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import org.bukkit.command.CommandSender;

import java.io.IOException;

public class SecretCommand extends SubCommand {
    public SecretCommand(TebexPlugin platform) {
        super(platform, "secret", "tebex.setup");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TebexPlugin platform = getPlatform();

        String serverToken = args[0];

        StoreSDK analyse = platform.getStoreSDK();
        ServerPlatformConfig analyseConfig = platform.getPlatformConfig();
        YamlDocument configFile = analyseConfig.getYamlDocument();

        analyse.setSecretKey(serverToken);

        platform.getStoreSDK().getServerInformation().thenAccept(serverInformation -> {
            analyseConfig.setSecretKey(serverToken);
            configFile.set("server.secret-key", serverToken);

            try {
                configFile.save();
            } catch (IOException e) {
                platform.sendMessage(sender, Lang.COMMAND_ERROR.getMessage(e.getLocalizedMessage()));
            }

            platform.loadServerPlatformConfig(configFile);
            platform.reloadConfig();
            platform.setBuyGUI(new BuyGUI(platform));
            platform.refreshListings();
            platform.configure();

            platform.sendMessage(sender, "Connected to &b" + serverInformation.getServer().getName() + "&7.");
        })
        .exceptionally(ex -> {
            Throwable cause = ex.getCause();

            if(cause instanceof NotFoundException) {
                platform.sendMessage(sender, "&cServer not found. Please check your secret key.");
                platform.halt();
                return null;
            }

            platform.sendMessage(sender, Lang.COMMAND_ERROR.getMessage(cause.getLocalizedMessage()));
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

    @Override
    public int getMinArgs() {
        return 1;
    }
}
