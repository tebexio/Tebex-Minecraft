package io.tebex.plugin.command.sub;

import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.sdk.obj.CheckoutUrl;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.ExecutionException;

public class SendLinkCommand extends SubCommand {
    public SendLinkCommand(TebexPlugin platform) {
        super(platform, "sendlink", "tebex.sendlink");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TebexPlugin platform = getPlatform();

        String username = args[0];
        int packageId = Integer.parseInt(args[0]);

        Player player = sender.getServer().getPlayer(username);
        if (player == null) {
            sender.sendMessage("§b[Tebex] §7Could not find a player with that name on the server.");
            return;
        }

        try {
            CheckoutUrl checkoutUrl = platform.getSDK().createCheckoutUrl(packageId, player.getUniqueId().toString()).get();
            sender.sendMessage("§b[Tebex] §7A checkout link has been created for you. Click here to complete payment: " + checkoutUrl.getUrl());
        } catch (InterruptedException|ExecutionException e) {
            sender.sendMessage("§b[Tebex] §7Failed to get checkout link for package: " + e.getMessage());
        }
    }

    @Override
    public String getDescription() {
        return "Creates payment link for a package and sends it to a player";
    }

    @Override
    public String getUsage() {
        return "<username> <packageId>";
    }
}
