package io.tebex.plugin.store.command.sub;

import com.velocitypowered.api.command.CommandSource;
import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;

import java.io.IOException;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

public class ReloadCommand extends SubCommand {
    public ReloadCommand(TebexPlugin platform) {
        super(platform, "reload", "tebex.admin");
    }

    @Override
    public void execute(CommandSource sender, String[] args) {
        TebexPlugin platform = getPlatform();
        try {
            YamlDocument configYaml = platform.initPlatformConfig();
            platform.loadServerPlatformConfig(configYaml);
            platform.refreshListings();
            sender.sendMessage(legacySection().deserialize("§8[Tebex] §7Successfully reloaded."));
        } catch (IOException e) {
            sender.sendMessage(legacySection().deserialize("§8[Tebex] §cFailed to reload the plugin: Check Console."));
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDescription() {
        return "Reloads the plugin.";
    }
}
