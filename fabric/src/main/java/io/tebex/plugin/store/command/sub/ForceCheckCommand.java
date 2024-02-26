package io.tebex.plugin.store.command.sub;

import com.mojang.brigadier.context.CommandContext;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.sdk.platform.PlatformLang;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

public class ForceCheckCommand extends SubCommand {
    public ForceCheckCommand(TebexPlugin platform) {
        super(platform, "forcecheck", "tebex.forcecheck");
    }

    @Override
    public void execute(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource sender = context.getSource();
        final TebexPlugin platform = getPlatform();

        if (! platform.isStoreSetup()) {
            platform.sendMessage(sender, PlatformLang.NOT_CONNECTED_TO_STORE.get());
            return;
        }

        // if running from console, return
        if (context.getSource().getEntity() != null) {
            platform.sendMessage(sender, "Performing force check...");
        }

        getPlatform().performCheck(false);
    }

    @Override
    public String getDescription() {
        return "Checks immediately for new purchases.";
    }
}
