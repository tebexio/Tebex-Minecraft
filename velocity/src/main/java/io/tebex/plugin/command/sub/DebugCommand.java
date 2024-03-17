package io.tebex.plugin.command.sub;

import com.velocitypowered.api.command.CommandSource;
import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.sdk.platform.config.ProxyPlatformConfig;
import io.tebex.sdk.util.StringUtil;
import net.kyori.adventure.text.Component;

import java.io.IOException;
import java.util.Set;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

public class DebugCommand extends SubCommand {
    public DebugCommand(TebexPlugin platform) {
        super(platform, "debug", "tebex.debug");
    }

    @Override
    public void execute(CommandSource sender, String[] args) {
        if (args.length != 1) { // require option
            sender.sendMessage(getInvalidUsageMessage());
            return;
        }

        TebexPlugin platform = getPlatform();

        ProxyPlatformConfig config = platform.getPlatformConfig();
        YamlDocument configFile = config.getYamlDocument();

        if (StringUtil.isTruthy(args[0])) {
            sender.sendMessage(legacySection().deserialize("§b[Tebex] §7Debug mode enabled."));
            config.setVerbose(true);
            configFile.set("verbose", true);
        } else if (StringUtil.isFalsy(args[0])){
            sender.sendMessage(legacySection().deserialize("§b[Tebex] §7Debug mode disabled."));
            config.setVerbose(false);
            configFile.set("verbose", false);
        } else {
            sender.sendMessage(getInvalidUsageMessage());
        }

        try {
            configFile.save();
        } catch (IOException e) {
            sender.sendMessage(legacySection().deserialize("§b[Tebex] §7Failed to save configuration file."));
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
