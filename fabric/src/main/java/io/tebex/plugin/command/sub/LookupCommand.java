package io.tebex.plugin.command.sub;

import com.mojang.brigadier.context.CommandContext;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.sdk.obj.PlayerLookupInfo;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

import java.util.concurrent.ExecutionException;

public class LookupCommand extends SubCommand {
    public LookupCommand(TebexPlugin platform) {
        super(platform, "lookup", "tebex.lookup");
    }

    @Override
    public void execute(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource source = context.getSource();
        TebexPlugin platform = getPlatform();

        if (!platform.isSetup()) {
            source.sendFeedback(new LiteralText("§b[Tebex] §7This server is not connected to a webstore. Use /tebex secret to set your store key."), false);
            return;
        }

        String username = context.getArgument("username", String.class);

        PlayerLookupInfo lookupInfo = null;
        try {
            lookupInfo = platform.getSDK().getPlayerLookupInfo(username).get();
        } catch (InterruptedException|ExecutionException e) {
            source.sendError(new LiteralText("§b[Tebex] §7Failed to complete player lookup. " + e.getMessage()));
            return;
        }

        source.sendFeedback(new LiteralText("§b[Tebex] §7Username: " + lookupInfo.getLookupPlayer().getUsername()), false);
        source.sendFeedback(new LiteralText("§b[Tebex] §7Id: " + lookupInfo.getLookupPlayer().getId()), false);
        source.sendFeedback(new LiteralText("§b[Tebex] §7Chargeback Rate: " + lookupInfo.chargebackRate), false);
        source.sendFeedback(new LiteralText("§b[Tebex] §7Bans Total: " + lookupInfo.banCount), false);
        source.sendFeedback(new LiteralText("§b[Tebex] §7Payments: " + lookupInfo.payments.size()), false);
    }

    @Override
    public String getDescription() {
        return "Gets user transaction info from your webstore.";
    }

    @Override
    public String getUsage() {
        return "<username>";
    }
}
