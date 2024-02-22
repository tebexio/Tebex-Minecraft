package io.tebex.plugin.store.command.sub;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.Lang;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import io.tebex.sdk.util.StringUtil;
import org.bukkit.command.CommandSender;

import java.io.IOException;

public class DebugCommand extends SubCommand {
    public DebugCommand(TebexPlugin platform) {
        super(platform, "debug", "tebex.debug");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TebexPlugin platform = getPlatform();

        ServerPlatformConfig config = platform.getPlatformConfig();
        YamlDocument configFile = config.getYamlDocument();

        if(args.length == 0) {
            config.setVerbose(! config.isVerbose());
        } else if(StringUtil.isTruthy(args[0]) || StringUtil.isFalsy(args[0])) {
            config.setVerbose(StringUtil.isTruthy(args[0]));
        } else {
            platform.sendMessage(sender, Lang.INVALID_USAGE.getMessage("tebex", getName() + " " + getUsage()));
            return;
        }

        configFile.set("verbose", config.isVerbose());

        if(config.isVerbose()) {
            platform.sendMessage(sender, "Debug mode enabled.");
        } else {
            platform.sendMessage(sender, "Debug mode disabled.");
        }

        try {
            configFile.save();
        } catch (IOException e) {
            platform.sendMessage(sender, "&cFailed to save configuration file.");
        }
    }

    @Override
    public String getDescription() {
        return "Enables more verbose logging.";
    }

    @Override
    public String getUsage() {
        return "[true/false/on/off]";
    }
}
