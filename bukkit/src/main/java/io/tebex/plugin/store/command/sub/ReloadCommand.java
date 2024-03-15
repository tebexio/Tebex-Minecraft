package io.tebex.plugin.store.command.sub;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.plugin.store.gui.BuyGUI;
import io.tebex.sdk.platform.PlatformLang;
import org.bukkit.command.CommandSender;

import java.io.IOException;

public class ReloadCommand extends SubCommand {
    public ReloadCommand(TebexPlugin platform) {
        super(platform, "reload", "tebex.admin");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        final TebexPlugin platform = getPlatform();

        try {
            YamlDocument configYaml = platform.initPlatformConfig();

            platform.loadServerPlatformConfig(configYaml);
            platform.reloadConfig();
            platform.getStoreManager().setBuyGui(new BuyGUI(platform));
            platform.refreshListings();

            platform.sendMessage(sender, PlatformLang.RELOAD_SUCCESS.get());
        } catch (IOException e) {
            platform.sendMessage(sender, PlatformLang.RELOAD_FAILURE.get());
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDescription() {
        return "Reloads the plugin.";
    }
}
