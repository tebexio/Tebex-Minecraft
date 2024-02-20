package io.tebex.plugin.command.analytics.sub;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import org.bukkit.command.CommandSender;

import java.io.IOException;

public class DebugCommand extends SubCommand {
    public DebugCommand(TebexPlugin platform) {
        super(platform, "debug", "analytics.debug");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TebexPlugin platform = getPlatform();
        ServerPlatformConfig analyseConfig = platform.getPlatformConfig();

        boolean debugEnabled = args.length > 0 ? Boolean.parseBoolean(args[0]) : !analyseConfig.isVerbose();

        if(debugEnabled) {
            getPlatform().sendMessage(sender, "You have enabled debug mode. This can be disabled by running &f/analytics debug &7again.");
        } else {
            getPlatform().sendMessage(sender, "You have disabled debug mode.");
        }

        YamlDocument configFile = analyseConfig.getYamlDocument();
        configFile.set("verbose", debugEnabled);
        analyseConfig.setVerbose(debugEnabled);

        try {
            configFile.save();
        } catch (IOException e) {
            getPlatform().sendMessage(sender, "&cFailed to toggle debug mode. Check console for more information.");
            e.printStackTrace();
        }
    }

    @Override
    public String getDescription() {
        return null;
    }
}
