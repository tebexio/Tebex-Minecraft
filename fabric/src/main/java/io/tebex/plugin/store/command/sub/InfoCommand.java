package io.tebex.plugin.store.command.sub;

import com.mojang.brigadier.context.CommandContext;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.sdk.platform.PlatformLang;
import net.minecraft.server.command.ServerCommandSource;

public class InfoCommand extends SubCommand {
    public InfoCommand(TebexPlugin platform) {
        super(platform, "info", "tebex.info");
    }

    @Override
    public void execute(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource sender = context.getSource();
        final TebexPlugin platform = getPlatform();

        if(! platform.isStoreSetup()) {
            platform.sendMessage(sender, PlatformLang.NOT_CONNECTED_TO_STORE.get());
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
