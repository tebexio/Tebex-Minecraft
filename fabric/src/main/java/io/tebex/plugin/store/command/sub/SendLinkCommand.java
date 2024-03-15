package io.tebex.plugin.store.command.sub;

import com.mojang.brigadier.context.CommandContext;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.sdk.exception.NotFoundException;
import io.tebex.sdk.platform.PlatformLang;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class SendLinkCommand extends SubCommand {
    public SendLinkCommand(TebexPlugin platform) {
        super(platform, "sendlink", "tebex.sendlink");
    }

    @Override
    public void execute(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource sender = context.getSource();
        final TebexPlugin platform = getPlatform();

        if (!platform.isStoreSetup()) {
            platform.sendMessage(sender, PlatformLang.NOT_CONNECTED_TO_STORE.get());
            return;
        }

        String username = context.getArgument("username", String.class).trim();
        Integer packageId = context.getArgument("packageId", Integer.class);

        ServerPlayerEntity player = context.getSource().getMinecraftServer().getPlayerManager().getPlayer(username);
        if (player == null) {
            platform.sendMessage(sender, PlatformLang.PLAYER_NOT_FOUND.get());
            return;
        }

        platform.getStoreSDK()
                .createCheckoutUrl(packageId, player.getName().asString())
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
        return "Creates payment link for a package and sends it to a player";
    }

    @Override
    public String getUsage() {
        return "<username> <packageId>";
    }
}
