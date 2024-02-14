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

        if (args.length != 2) {
            platform.sendMessage(sender, "&cInvalid command usage. Use /tebex " + this.getName() + " " + getUsage());
            return;
        }

        String username = args[0].trim();
        try {
            Player player = sender.getServer().getPlayer(username);
            if (player == null) {
                sender.sendMessage("&cCould not find a player with that name on the server.");
                return;
            }

            int packageId = Integer.parseInt(args[1]);
            CheckoutUrl checkoutUrl = platform.getSDK().createCheckoutUrl(packageId, username).get();
            player.sendMessage("A checkout link has been created for you. Click here to complete payment: " + checkoutUrl.getUrl());
        } catch (InterruptedException|ExecutionException e) {
            platform.sendMessage(sender, "&cFailed to get checkout link for package: " + e.getMessage());
        } catch (NumberFormatException e) {
            platform.sendMessage(sender, "&cPackage ID must be a number.");
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
