package io.tebex.plugin.command.sub;

import io.tebex.plugin.Lang;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.sdk.exception.NotFoundException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SendLinkCommand extends SubCommand {
    public SendLinkCommand(TebexPlugin platform) {
        super(platform, "sendlink", "tebex.sendlink");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TebexPlugin platform = getPlatform();

        String username = args[0];

        Player player = sender.getServer().getPlayer(username);
        if (player == null) {
            platform.sendMessage(sender, "&cCould not find a player with that name on the server.");
            return;
        }

        int packageId;
        try {
             packageId = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            platform.sendMessage(sender, "&cPackage ID must be a number.");
            return;
        }

        platform.getStoreSDK()
                .createCheckoutUrl(packageId, username)
                .thenAccept(checkoutUrl -> {
                    platform.sendMessage(sender, Lang.CHECKOUT_URL.getMessage(checkoutUrl.getUrl()));
                })
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause();

                    if(cause instanceof NotFoundException) {
                        platform.sendMessage(sender, "&cPackage not found. Please check the package ID.");
                        return null;
                    }

                    platform.sendMessage(sender, Lang.COMMAND_ERROR.getMessage(cause.getLocalizedMessage()));
                    return null;
                });
    }

    @Override
    public String getDescription() {
        return "Creates payment link for a package and sends it to a player.";
    }

    @Override
    public String getUsage() {
        return "<username> <packageId>";
    }

    @Override
    public int getMinArgs() {
        return 2;
    }
}
