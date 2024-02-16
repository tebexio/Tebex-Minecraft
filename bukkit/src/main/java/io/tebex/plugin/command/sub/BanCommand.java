package io.tebex.plugin.command.sub;

import io.tebex.plugin.Lang;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import org.bukkit.command.CommandSender;

import java.util.concurrent.ExecutionException;

public class BanCommand extends SubCommand {
    public BanCommand(TebexPlugin platform) {
        super(platform, "ban", "tebex.ban");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TebexPlugin platform = getPlatform();

        if (!platform.isSetup()) {
            platform.sendMessage(sender, Lang.NOT_CONNECTED.getMessage());
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

        try {
            boolean success = platform.getSDK().createBan(playerName, ip, reason).get();

            if (!success) {
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
        return "<username> [reason] [ip]";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }
}
