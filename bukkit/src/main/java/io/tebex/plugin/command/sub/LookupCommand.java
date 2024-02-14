package io.tebex.plugin.command.sub;

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
            platform.sendMessage(sender, "&cThis server is not connected to a webstore. Use /tebex secret to set your store key.");
            return;
        }

        if (args.length != 1) {
            platform.sendMessage(sender, "&cInvalid command usage. Use /tebex " + this.getName() + " " + getUsage());
            return;
        }

        String username = args[0];

        PlayerLookupInfo lookupInfo;
        try {
            CompletableFuture<PlayerLookupInfo> future = platform.getSDK().getPlayerLookupInfo(username);
            lookupInfo = future.get();
        } catch (InterruptedException|ExecutionException e) {
            platform.sendMessage(sender, "Failed to complete player lookup. " + e.getMessage());
            return;
        }

        if(lookupInfo == null) {
            platform.sendMessage(sender, "No information found for that player.");
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
