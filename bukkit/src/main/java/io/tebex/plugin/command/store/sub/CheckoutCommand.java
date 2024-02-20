package io.tebex.plugin.command.sub;

import io.tebex.plugin.Lang;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.sdk.store.obj.CheckoutUrl;
import org.bukkit.command.CommandSender;

import java.util.concurrent.ExecutionException;

public class CheckoutCommand extends SubCommand {
    public CheckoutCommand(TebexPlugin platform) {
        super(platform, "checkout", "tebex.checkout");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TebexPlugin platform = getPlatform();

        if (!platform.isStoreSetup()) {
            platform.sendMessage(sender, Lang.NOT_CONNECTED_TO_STORE.getMessage());
            return;
        }

        try {
            int packageId = Integer.parseInt(args[0]);
            CheckoutUrl checkoutUrl = platform.getStoreSDK().createCheckoutUrl(packageId, sender.getName()).get();
            platform.sendMessage(sender, Lang.CHECKOUT_URL.getMessage(checkoutUrl.getUrl()));
        } catch (InterruptedException|ExecutionException e) {
            platform.sendMessage(sender, Lang.FAILED_TO_CREATE_CHECKOUT_URL.getMessage());
        }
    }

    @Override
    public String getDescription() {
        return "Creates payment link for a package";
    }

    @Override
    public String getUsage() {
        return "<packageId>";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }
}
