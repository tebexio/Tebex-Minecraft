package io.tebex.plugin.store.command.sub;

import com.mojang.brigadier.context.CommandContext;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.sdk.platform.PlatformLang;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.ExecutionException;

public class BanCommand extends SubCommand {
    public BanCommand(TebexPlugin platform) {
        super(platform, "ban", "tebex.ban");
    }

    @Override
    public void execute(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource sender = context.getSource();
        final TebexPlugin platform = getPlatform();

        if (! platform.isStoreSetup()) {
            platform.sendMessage(sender, PlatformLang.NOT_CONNECTED_TO_STORE.get());
            return;
        }

        String playerName = context.getArgument("playerName", String.class);
        String reason = "";
        String ip = "";

        try {
            reason = context.getArgument("reason", String.class);
            ip = context.getArgument("ip", String.class);
        } catch (IllegalArgumentException ignored) {}

        try {
            boolean success = platform.getStoreSDK().createBan(playerName, ip, reason).get();

            if(! success) {
                platform.sendMessage(sender, "&cThat player is already banned.");
                return;
            }

            platform.sendMessage(sender, "&7Player banned successfully.");
        } catch (InterruptedException | ExecutionException e) {
            platform.sendMessage(sender, "&cError while banning player: &f" + e.getMessage());
        }
    }

    @Override
    public String getDescription() {
        return "Bans a player from using the webstore. Unbans can only be made via the web panel.";
    }

    @Override
    public String getUsage() {
        return "<playerName> <opt:reason> <opt:ip>";
    }
}
