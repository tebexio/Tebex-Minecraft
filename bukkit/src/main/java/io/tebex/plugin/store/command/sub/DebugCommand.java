package io.tebex.plugin.store.command.sub;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.sdk.platform.PlatformLang;
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

        String input = args.length > 0 ? args[0] : null;
        if(input == null) {
            config.setVerbose(! config.isVerbose());
        } else if (StringUtil.isTruthy(input)) {
            config.setVerbose(true);
        } else if (StringUtil.isFalsy(input)) {
            config.setVerbose(false);
        } else {
            platform.sendMessage(sender, PlatformLang.INVALID_USAGE.get("tebex", getName() + " " + getUsage()));
            return;
        }

        configFile.set("verbose", config.isVerbose());

        try {
            configFile.save();
        } catch (IOException e) {
            platform.sendMessage(sender, "&cFailed to save configuration file.");
            return;
        }

        if(config.isVerbose()) {
            platform.sendMessage(sender, "Debug mode enabled.");
            return;
        }

        platform.sendMessage(sender, "Debug mode disabled.");
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
