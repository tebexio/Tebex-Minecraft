package io.tebex.plugin.command.sub;

import com.mojang.brigadier.context.CommandContext;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

public class InfoCommand extends SubCommand {
    public InfoCommand(TebexPlugin platform) {
        super(platform, "info", "tebex.info");
    }

    @Override
    public void execute(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource source = context.getSource();
        TebexPlugin platform = getPlatform();

        if (platform.isStoreSetup()) {
            source.sendFeedback(new LiteralText("§b[Tebex] §7Information for this server:"), false);
            source.sendFeedback(new LiteralText("§b[Tebex] §7" + platform.getStoreInformation().getServer().getName() + " for webstore " + platform.getStoreInformation().getStore().getName()), false);
            source.sendFeedback(new LiteralText("§b[Tebex] §7Server prices are in " +  platform.getStoreInformation().getStore().getCurrency().getIso4217()), false);
            source.sendFeedback(new LiteralText("§b[Tebex] §7Webstore domain " +  platform.getStoreInformation().getStore().getDomain()), false);
        } else {
            source.sendFeedback(new LiteralText("§b[Tebex] §7This server is not connected to a webstore. Use /tebex secret to set your store key."), false);
        }
    }

    @Override
    public String getDescription() {
        return "Gets information about this server's connected store.";
    }
}
