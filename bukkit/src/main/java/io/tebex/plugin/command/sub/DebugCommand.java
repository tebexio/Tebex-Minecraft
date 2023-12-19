package io.tebex.plugin.command.sub;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import org.bukkit.command.CommandSender;

public class DebugCommand extends SubCommand {
    public DebugCommand(TebexPlugin platform) {
        super(platform, "debug", "tebex.debug");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TebexPlugin platform = getPlatform();

        ServerPlatformConfig config = platform.getPlatformConfig();
        YamlDocument configFile = config.getYamlDocument();

        boolean enableDebug = Boolean.parseBoolean(args[0]);
        if (enableDebug) {
            sender.sendMessage("§b[Tebex] §7Debug mode enabled.");
            config.setVerbose(true);
            configFile.set("verbose", true);
        } else {
            sender.sendMessage("§b[Tebex] §7Debug mode disabled.");
            config.setVerbose(false);
            configFile.set("verbose", false);
        }
    }

    @Override
    public String getDescription() {
        return "Enables more verbose logging.";
    }

    @Override
    public String getUsage() {
        return "<trueOrFalse>";
    }
}
