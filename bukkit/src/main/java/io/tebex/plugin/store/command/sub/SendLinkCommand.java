package io.tebex.plugin.store.command.sub;

import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.sdk.exception.NotFoundException;
import io.tebex.sdk.platform.PlatformLang;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SendLinkCommand extends SubCommand {
    public SendLinkCommand(TebexPlugin platform) {
        super(platform, "sendlink", "tebex.sendlink");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        final TebexPlugin platform = getPlatform();

        Player player = sender.getServer().getPlayer(args[0]);
        if (player == null) {
            platform.sendMessage(sender, PlatformLang.PLAYER_NOT_FOUND.get());
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
                .createCheckoutUrl(packageId, player.getName())
                .thenAccept(checkoutUrl -> {
                    platform.sendMessage(sender, PlatformLang.CHECKOUT_URL.get(checkoutUrl.getUrl()));
                })
                .exceptionally(ex -> {
                    Throwable cause = ex.getCause();

                    if(cause instanceof NotFoundException) {
                        platform.sendMessage(sender, "&cPackage not found. Please check the package ID.");
                        return null;
                    }

                    platform.sendMessage(sender, PlatformLang.ERROR_OCCURRED.get(cause.getLocalizedMessage()));
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
