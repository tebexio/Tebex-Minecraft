package io.tebex.plugin.command.sub;

import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.sdk.obj.CheckoutUrl;
import org.bukkit.command.CommandSender;

import java.util.concurrent.ExecutionException;

public class CheckoutCommand extends SubCommand {
    public CheckoutCommand(TebexPlugin platform) {
        super(platform, "checkout", "tebex.checkout");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TebexPlugin platform = getPlatform();

        if (!platform.isSetup()) {
            sender.sendMessage("§b[Tebex] §7This server is not connected to a webstore. Use /tebex.secret to set your store key.");
            return;
        }

        int packageId = Integer.parseInt(args[0]);
        try {
            CheckoutUrl checkoutUrl = platform.getSDK().createCheckoutUrl(packageId, sender.getName()).get();
            sender.sendMessage("§b[Tebex] §7Checkout started! Click here to complete payment: " + checkoutUrl.getUrl());
        } catch (InterruptedException|ExecutionException e) {
            sender.sendMessage("§b[Tebex] §7Failed to get checkout link for package: " + e.getMessage());
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
}
