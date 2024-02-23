package io.tebex.plugin.store.command.store;

import com.mojang.brigadier.context.CommandContext;
import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import io.tebex.sdk.util.StringUtil;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

import java.io.IOException;

public class DebugCommand extends SubCommand {
    public DebugCommand(TebexPlugin platform) {
        super(platform, "debug", "tebex.debug");
    }

    @Override
    public void execute(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource source = context.getSource();
        TebexPlugin platform = getPlatform();

        ServerPlatformConfig config = platform.getPlatformConfig();
        YamlDocument configFile = config.getYamlDocument();

        String input = context.getArgument("trueOrFalse", String.class);
        if (StringUtil.isTruthy(input)) {
            context.getSource().sendFeedback(new LiteralText("§b[Tebex] §7Debug mode enabled."), false);
            config.setVerbose(true);
            configFile.set("verbose", true);
        } else if (StringUtil.isFalsy(input)) {
            context.getSource().sendFeedback(new LiteralText("§b[Tebex] §7Debug mode disabled."), false);
            config.setVerbose(false);
            configFile.set("verbose", false);
        } else {
            context.getSource().sendFeedback(new LiteralText("§b[Tebex] §7Invalid command usage. Use /tebex " + this.getName() + " " + getUsage()), false);
        }

        try {
            configFile.save();
        } catch (IOException e) {
            context.getSource().sendFeedback(new LiteralText("§b[Tebex] §7Failed to save configuration file."), false);
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
