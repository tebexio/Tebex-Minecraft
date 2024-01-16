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
            sender.sendMessage("§b[Tebex] §7Information for this server:");
            sender.sendMessage("§b[Tebex] §7" + platform.getStoreInformation().getServer().getName() + " for webstore " + platform.getStoreInformation().getStore().getName());
            sender.sendMessage("§b[Tebex] §7Server prices are in " +  platform.getStoreInformation().getStore().getCurrency().getIso4217());
            sender.sendMessage("§b[Tebex] §7Webstore domain " +  platform.getStoreInformation().getStore().getDomain());
        } else {
            sender.sendMessage("§b[Tebex] §7This server is not connected to a webstore. Use /tebex secret to set your store key.");
        }
    }

    @Override
    public String getDescription() {
        return "Gets information about this server's connected store.";
    }
}
