package io.tebex.plugin.store.command.sub;

import com.mojang.brigadier.context.CommandContext;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.sdk.platform.PlatformLang;
import io.tebex.sdk.store.obj.CheckoutUrl;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

import java.util.concurrent.ExecutionException;

public class CheckoutCommand extends SubCommand {
    public CheckoutCommand(TebexPlugin platform) {
        super(platform, "checkout", "tebex.checkout");
    }

    @Override
    public void execute(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource sender = context.getSource();
        final TebexPlugin platform = getPlatform();

        if (!platform.isStoreSetup()) {
            platform.sendMessage(sender, PlatformLang.NOT_CONNECTED_TO_STORE.get());
            return;
        }

        Integer packageId = context.getArgument("packageId", Integer.class);
        try {
            CheckoutUrl checkoutUrl = platform.getStoreSDK().createCheckoutUrl(packageId, context.getSource().getName()).get();
            platform.sendMessage(sender, PlatformLang.CHECKOUT_URL.get(checkoutUrl.getUrl()));
        } catch (InterruptedException|ExecutionException e) {
            context.getSource().sendError(new LiteralText("§b[Tebex] §7Failed to get checkout link for package: " + e.getMessage()));
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
