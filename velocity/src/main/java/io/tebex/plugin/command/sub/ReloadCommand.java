package io.tebex.plugin.command.sub;

import com.velocitypowered.api.command.CommandSource;
import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.IOException;

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
            sender.sendMessage(Component.text("[Tebex] ").color(NamedTextColor.DARK_GRAY).append(Component.text("Successfully reloaded.").color(NamedTextColor.GRAY)));
        } catch (IOException e) {
            sender.sendMessage(Component.text("[Tebex] ").color(NamedTextColor.DARK_GRAY).append(Component.text("Failed to reload the plugin: Check Console.").color(NamedTextColor.RED)));
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDescription() {
        return "Reloads the plugin.";
    }
}
