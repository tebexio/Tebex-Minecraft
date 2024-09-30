package io.tebex.plugin.command.sub;

import com.mojang.brigadier.context.CommandContext;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.sdk.obj.CheckoutUrl;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.concurrent.ExecutionException;

public class SendLinkCommand extends SubCommand {
    public SendLinkCommand(TebexPlugin platform) {
        super(platform, "sendlink", "tebex.sendlink");
    }

    @Override
    public void execute(CommandContext<ServerCommandSource> context) {
        TebexPlugin platform = getPlatform();

        String username = context.getArgument("username", String.class).trim();
        Integer packageId = context.getArgument("packageId", Integer.class);

        ServerPlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(username);
        if (player == null) {
            context.getSource().sendError(Text.of("§b[Tebex] §7Could not find a player with that name on the server."));
            return;
        }

        try {
            CheckoutUrl checkoutUrl = platform.getSDK().createCheckoutUrl(packageId, username).get();
            player.sendMessage(Text.of("§b[Tebex] §7A checkout link has been created for you. Click here to complete payment: " + checkoutUrl.getUrl()), false);
        } catch (InterruptedException|ExecutionException e) {
            context.getSource().sendError(Text.of("§b[Tebex] §7Failed to get checkout link for package: " + e.getMessage()));
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
