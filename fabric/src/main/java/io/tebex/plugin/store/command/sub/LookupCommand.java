package io.tebex.plugin.store.command.sub;

import com.mojang.brigadier.context.CommandContext;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.sdk.platform.PlatformLang;
import io.tebex.sdk.store.obj.PlayerLookupInfo;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class LookupCommand extends SubCommand {
    public LookupCommand(TebexPlugin platform) {
        super(platform, "lookup", "tebex.lookup");
    }

    @Override
    public void execute(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource sender = context.getSource();
        final TebexPlugin platform = getPlatform();

        if (! platform.isStoreSetup()) {
            platform.sendMessage(sender, PlatformLang.NOT_CONNECTED_TO_STORE.get());
            return;
        }

        String username = context.getArgument("username", String.class);

        PlayerLookupInfo lookupInfo;
        try {
            CompletableFuture<PlayerLookupInfo> future = platform.getStoreSDK().getPlayerLookupInfo(username);
            lookupInfo = future.get();
        } catch (InterruptedException|ExecutionException e) {
            platform.sendMessage(sender, "&cFailed to complete player lookup. " + e.getMessage());
            return;
        }

        if(lookupInfo == null) {
            platform.sendMessage(sender, "&cNo information found for that player.");
            return;
        }

        platform.sendMessage(sender, "Username: " + lookupInfo.getLookupPlayer().getUsername());
        platform.sendMessage(sender, "Id: " + lookupInfo.getLookupPlayer().getId());
        platform.sendMessage(sender, "Chargeback Rate: " + lookupInfo.chargebackRate);
        platform.sendMessage(sender, "Bans Total: " + lookupInfo.banCount);
        platform.sendMessage(sender, "Payments: " + lookupInfo.payments.size());
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
