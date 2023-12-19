package io.tebex.plugin.command.sub;

import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.sdk.obj.PlayerLookupInfo;
import org.bukkit.command.CommandSender;

import java.util.concurrent.ExecutionException;

public class LookupCommand extends SubCommand {
    public LookupCommand(TebexPlugin platform) {
        super(platform, "lookup", "tebex.lookup");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TebexPlugin platform = getPlatform();

        if (!platform.isSetup()) {
            sender.sendMessage("§b[Tebex] §7This server is not connected to a webstore. Use /tebex.secret to set your store key.");
            return;
        }

        String username = args[0];

        PlayerLookupInfo lookupInfo = null;
        try {
            lookupInfo = platform.getSDK().getPlayerLookupInfo(username).get();
        } catch (InterruptedException|ExecutionException e) {
            sender.sendMessage("§b[Tebex] §7Failed to complete player lookup. " + e.getMessage());
            return;
        }

        sender.sendMessage("§b[Tebex] §7Username: " + lookupInfo.getLookupPlayer().getUsername());
        sender.sendMessage("§b[Tebex] §7Id: " + lookupInfo.getLookupPlayer().getId());
        sender.sendMessage("§b[Tebex] §7Chargeback Rate: " + lookupInfo.chargebackRate);
        sender.sendMessage("§b[Tebex] §7Bans Total: " + lookupInfo.banCount);
        sender.sendMessage("§b[Tebex] §7Payments: " + lookupInfo.payments.size());
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
