package io.tebex.plugin.command.sub;

import com.velocitypowered.api.command.CommandSource;
import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.sdk.platform.config.ProxyPlatformConfig;
import io.tebex.sdk.util.StringUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.IOException;

public class DebugCommand extends SubCommand {
    public DebugCommand(TebexPlugin platform) {
        super(platform, "debug", "tebex.debug");
    }

    @Override
    public void execute(CommandSource sender, String[] args) {
        TebexPlugin platform = getPlatform();

        ProxyPlatformConfig config = platform.getPlatformConfig();
        YamlDocument configFile = config.getYamlDocument();

        if (StringUtil.isTruthy(args[0])) {
            sender.sendMessage(Component.text("[Tebex] ").color(NamedTextColor.AQUA).append(Component.text("Debug mode enabled.").color(NamedTextColor.GRAY)));
            config.setVerbose(true);
            configFile.set("verbose", true);
        } else if (StringUtil.isFalsy(args[0])){
            sender.sendMessage(Component.text("[Tebex] ").color(NamedTextColor.AQUA).append(Component.text("Debug mode disabled.").color(NamedTextColor.GRAY)));
            config.setVerbose(false);
            configFile.set("verbose", false);
        } else {
            sender.sendMessage(Component.text("[Tebex] ").color(NamedTextColor.AQUA).append(Component.text("Invalid command usage. Use /tebex " + this.getName() + " ").color(NamedTextColor.GRAY).append(LegacyComponentSerializer.legacyAmpersand().deserialize(getUsage()))));
        }

        try {
            configFile.save();
        } catch (IOException e) {
            sender.sendMessage(Component.text("[Tebex] ").color(NamedTextColor.AQUA).append(Component.text("Failed to save configuration file.").color(NamedTextColor.GRAY)));
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
