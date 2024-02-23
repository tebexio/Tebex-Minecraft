package io.tebex.plugin.store.command.sub;

import com.mojang.brigadier.context.CommandContext;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

import java.util.concurrent.ExecutionException;

public class BanCommand extends SubCommand {
    public BanCommand(TebexPlugin platform) {
        super(platform, "ban", "tebex.ban");
    }

    @Override
    public void execute(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource source = context.getSource();
        TebexPlugin platform = getPlatform();

        String playerName = context.getArgument("playerName", String.class);
        String reason = "";
        String ip = "";

        try {
            reason = context.getArgument("reason", String.class);
            ip = context.getArgument("ip", String.class);
        } catch (IllegalArgumentException ignored) {}

        if (!platform.isStoreSetup()) {
            source.sendFeedback(new LiteralText("§b[Tebex] §7This server is not connected to a webstore. Use /tebex secret to set your store key."), false);
            return;
        }

        try {
            boolean success = platform.getStoreSDK().createBan(playerName, ip, reason).get();
            if (success) {
                source.sendFeedback(new LiteralText("§b[Tebex] §7Player banned successfully."), false);
            } else {
                source.sendFeedback(new LiteralText("§b[Tebex] §7Failed to ban player."), false);
            }
        } catch (InterruptedException | ExecutionException e) {
            source.sendFeedback(new LiteralText("§b[Tebex] §7Error while banning player: " + e.getMessage()), false);
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
