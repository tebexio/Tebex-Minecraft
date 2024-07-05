package io.tebex.plugin.command.sub;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
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

        if (args.length != 1) {
            sender.sendMessage("§b[Tebex] §7Invalid command usage. Use /tebex " + this.getName() + " " + getUsage());
            return;
        }

        if (StringUtil.isTruthy(args[0])) {
            sender.sendMessage("§b[Tebex] §7Debug mode enabled.");
            config.setVerbose(true);
            configFile.set("verbose", true);
        } else if (StringUtil.isFalsy(args[0])) {
            sender.sendMessage("§b[Tebex] §7Debug mode disabled.");
            config.setVerbose(false);
            configFile.set("verbose", false);
        } else {
            sender.sendMessage("§b[Tebex] §7Invalid command usage. Use /tebex " + this.getName() + " " + getUsage());
        }

        try {
            configFile.save();
        } catch (IOException e) {
            sender.sendMessage("§b[Tebex] §7Failed to save configuration file.");
        }
    }

    @Override
    public String getDescription() {
        return "Enables more verbose logging.";
    }

    @Override
    public String getUsage() {
        return "<true/false/on/off>";
    }
}
