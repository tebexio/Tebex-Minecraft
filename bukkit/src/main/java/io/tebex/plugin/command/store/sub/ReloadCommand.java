package io.tebex.plugin.command.store.sub;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.Lang;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.plugin.gui.BuyGUI;
import org.bukkit.command.CommandSender;

import java.io.IOException;

public class ReloadCommand extends SubCommand {
    public ReloadCommand(TebexPlugin platform) {
        super(platform, "reload", "tebex.admin");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TebexPlugin platform = getPlatform();

        try {
            YamlDocument configYaml = platform.initPlatformConfig();

            platform.loadServerPlatformConfig(configYaml);
            platform.reloadConfig();
            platform.getStoreManager().setBuyGui(new BuyGUI(platform));
            platform.refreshListings();

            platform.sendMessage(sender, Lang.RELOAD_SUCCESS.getMessage());
        } catch (IOException e) {
            platform.sendMessage(sender, Lang.RELOAD_FAILURE.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDescription() {
        return "Reloads the plugin.";
    }
}
