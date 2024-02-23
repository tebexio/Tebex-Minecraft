package io.tebex.plugin.store.command.sub;

import io.tebex.plugin.util.Lang;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import org.bukkit.command.CommandSender;

public class InfoCommand extends SubCommand {
    public InfoCommand(TebexPlugin platform) {
        super(platform, "info", "tebex.info");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TebexPlugin platform = getPlatform();

        if(! platform.isStoreSetup()) {
            platform.sendMessage(sender, Lang.NOT_CONNECTED_TO_STORE.get());
            return;
        }

        platform.sendMessage(sender, "Information for this server:");
        platform.sendMessage(sender, platform.getStoreInformation().getServer().getName() + " for webstore " + platform.getStoreInformation().getStore().getName());
        platform.sendMessage(sender, "Server prices are in " +  platform.getStoreInformation().getStore().getCurrency().getIso4217());
        platform.sendMessage(sender, "Webstore domain " +  platform.getStoreInformation().getStore().getDomain());
    }

    @Override
    public String getDescription() {
        return "Gets information about this server's connected store.";
    }
}
