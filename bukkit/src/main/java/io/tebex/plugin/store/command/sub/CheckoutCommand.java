package io.tebex.plugin.store.command.sub;

import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.sdk.platform.PlatformLang;
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
            platform.sendMessage(sender, PlatformLang.NOT_CONNECTED_TO_STORE.get());
            return;
        }

        try {
            int packageId = Integer.parseInt(args[0]);
            CheckoutUrl checkoutUrl = platform.getStoreSDK().createCheckoutUrl(packageId, sender.getName()).get();
            platform.sendMessage(sender, PlatformLang.CHECKOUT_URL.get(checkoutUrl.getUrl()));
        } catch (InterruptedException|ExecutionException e) {
            platform.sendMessage(sender, PlatformLang.FAILED_TO_CREATE_CHECKOUT_URL.get());
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
