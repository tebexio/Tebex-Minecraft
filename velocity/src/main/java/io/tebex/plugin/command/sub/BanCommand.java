package io.tebex.plugin.command.sub;

import com.velocitypowered.api.command.CommandSource;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;

import java.util.concurrent.ExecutionException;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

public class BanCommand extends SubCommand {
    public BanCommand(TebexPlugin platform) {
        super(platform, "ban", "tebex.ban");
    }

    @Override
    public void execute(CommandSource sender, String[] args) {
        TebexPlugin platform = getPlatform();

        if (args.length < 1) { // require username at minimum
            sender.sendMessage(getInvalidUsageMessage());
            return;
        }

        String playerName = args[0];
        String reason = "";
        String ip = "";

        if (args.length > 1) { // second param provided
            reason = args[1];
        }
        if (args.length > 2) { // third param provided
            ip = args[2];
        }

        if (!platform.isSetup()) {
            sender.sendMessage(legacySection().deserialize("§b[Tebex] §7This server is not connected to a webstore. Use /tebex secret to set your store key."));
            return;
        }

        try {
            boolean success = platform.getSDK().createBan(playerName, ip, reason).get();
            if (success) {
                sender.sendMessage(legacySection().deserialize("§b[Tebex] §7Player banned successfully."));
            } else {
                sender.sendMessage(legacySection().deserialize("§b[Tebex] §7Failed to ban player."));
            }
        } catch (InterruptedException | ExecutionException e) {
            sender.sendMessage(legacySection().deserialize("§b[Tebex] §7Error while banning player: " + e.getMessage()));
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
