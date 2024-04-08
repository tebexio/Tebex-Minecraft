package io.tebex.plugin.store.command.sub;

import com.mojang.brigadier.context.CommandContext;
import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.sdk.platform.PlatformLang;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import io.tebex.sdk.util.StringUtil;
import net.minecraft.server.command.ServerCommandSource;

import java.io.IOException;

public class DebugCommand extends SubCommand {
    public DebugCommand(TebexPlugin platform) {
        super(platform, "debug", "tebex.debug");
    }

    @Override
    public void execute(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource sender = context.getSource();
        final TebexPlugin platform = getPlatform();

        ServerPlatformConfig config = platform.getPlatformConfig();
        YamlDocument configFile = config.getYamlDocument();

        String input = context.getArgument("trueOrFalse", String.class);
        if (StringUtil.isTruthy(input)) {
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
        return "<trueOrFalse>";
    }
}
