package io.tebex.plugin.command.sub;

import io.tebex.plugin.CommonMessages;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.sdk.obj.PlayerLookupInfo;
import org.bukkit.command.CommandSender;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class LookupCommand extends SubCommand {
    public LookupCommand(TebexPlugin platform) {
        super(platform, "lookup", "tebex.lookup");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TebexPlugin platform = getPlatform();

        if (!platform.isSetup()) {
            platform.sendMessage(sender, CommonMessages.NOT_CONNECTED.getMessage());
            return;
        }

        String username = args[0];

        PlayerLookupInfo lookupInfo;
        try {
            CompletableFuture<PlayerLookupInfo> future = platform.getSDK().getPlayerLookupInfo(username);
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

    @Override
    public int getMinArgs() {
        return 1;
    }
}
