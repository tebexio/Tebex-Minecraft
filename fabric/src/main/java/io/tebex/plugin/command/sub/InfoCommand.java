package io.tebex.plugin.command.sub;

import com.mojang.brigadier.context.CommandContext;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class InfoCommand extends SubCommand {
    public InfoCommand(TebexPlugin platform) {
        super(platform, "info", "tebex.info");
    }

    @Override
    public void execute(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource source = context.getSource();
        TebexPlugin platform = getPlatform();

        if (platform.isSetup()) {
            source.sendMessage(Text.of("§b[Tebex] §7Information for this server:"));
            source.sendMessage(Text.of("§b[Tebex] §7" + platform.getStoreInformation().getServer().getName() + " for webstore " + platform.getStoreInformation().getStore().getName()));
            source.sendMessage(Text.of("§b[Tebex] §7Server prices are in " +  platform.getStoreInformation().getStore().getCurrency().getIso4217()));
            source.sendMessage(Text.of("§b[Tebex] §7Webstore domain " +  platform.getStoreInformation().getStore().getDomain()));
        } else {
            source.sendMessage(Text.of("§b[Tebex] §7This server is not connected to a webstore. Use /tebex secret to set your store key."));
        }
    }

    @Override
    public String getDescription() {
        return "Gets information about this server's connected store.";
    }
}
