package io.tebex.plugin.command.sub;

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

        if (args.length < 1) { // require username at minimum
            sender.sendMessage("&cInvalid command usage. Use /tebex " + this.getName() + " " + getUsage());
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
            sender.sendMessage("&cThis server is not connected to a webstore. Use /tebex secret to set your store key.");
            return;
        }

        try {
            boolean success = platform.getSDK().createBan(playerName, ip, reason).get();
            if (success) {
                sender.sendMessage("&7Player banned successfully.");
            } else {
                sender.sendMessage("&cFailed to ban player.");
            }
        } catch (InterruptedException | ExecutionException e) {
            sender.sendMessage("&cError while banning player: &f" + e.getMessage());
        }
    }

    @Override
    public String getDescription() {
        return "Bans a player from using the webstore. Unbans can only be made via the web panel.";
    }

    @Override
    public String getUsage() {
        return "<playerName> <reason> <ip>";
    }
}
