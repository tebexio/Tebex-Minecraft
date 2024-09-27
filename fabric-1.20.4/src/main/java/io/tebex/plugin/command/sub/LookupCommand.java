package io.tebex.plugin.command.sub;

import com.mojang.brigadier.context.CommandContext;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.sdk.obj.PlayerLookupInfo;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

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
            source.sendMessage(Text.of("§b[Tebex] §7This server is not connected to a webstore. Use /tebex secret to set your store key."));
            return;
        }

        String username = context.getArgument("username", String.class);

        PlayerLookupInfo lookupInfo = null;
        try {
            lookupInfo = platform.getSDK().getPlayerLookupInfo(username).get();
        } catch (InterruptedException|ExecutionException e) {
            source.sendError(Text.of("§b[Tebex] §7Failed to complete player lookup. " + e.getMessage()));
            return;
        }

        source.sendMessage(Text.of("§b[Tebex] §7Username: " + lookupInfo.getLookupPlayer().getUsername()));
        source.sendMessage(Text.of("§b[Tebex] §7Id: " + lookupInfo.getLookupPlayer().getId()));
        source.sendMessage(Text.of("§b[Tebex] §7Chargeback Rate: " + lookupInfo.chargebackRate));
        source.sendMessage(Text.of("§b[Tebex] §7Bans Total: " + lookupInfo.banCount));
        source.sendMessage(Text.of("§b[Tebex] §7Payments: " + lookupInfo.payments.size()));
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
