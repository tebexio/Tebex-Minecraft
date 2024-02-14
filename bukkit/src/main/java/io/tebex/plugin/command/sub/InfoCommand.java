package io.tebex.plugin.command.sub;

import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import org.bukkit.command.CommandSender;

public class InfoCommand extends SubCommand {
    public InfoCommand(TebexPlugin platform) {
        super(platform, "info", "tebex.info");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TebexPlugin platform = getPlatform();

        if (platform.isSetup()) {
            platform.sendMessage(sender, "Information for this server:");
            platform.sendMessage(sender, platform.getStoreInformation().getServer().getName() + " for webstore " + platform.getStoreInformation().getStore().getName());
            platform.sendMessage(sender, "Server prices are in " +  platform.getStoreInformation().getStore().getCurrency().getIso4217());
            platform.sendMessage(sender, "Webstore domain " +  platform.getStoreInformation().getStore().getDomain());
        } else {
            platform.sendMessage(sender, "&cThis server is not connected to a webstore. Use /tebex secret to set your store key.");
        }
    }

    @Override
    public String getDescription() {
        return "Gets information about this server's connected store.";
    }
}
