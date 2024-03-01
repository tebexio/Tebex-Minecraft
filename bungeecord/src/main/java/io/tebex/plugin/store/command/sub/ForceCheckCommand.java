package io.tebex.plugin.store.command.sub;

import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;

public class ForceCheckCommand extends SubCommand {
    private final TebexPlugin platform;

    public ForceCheckCommand(TebexPlugin platform) {
        super(platform, "forcecheck", "tebex.forcecheck");
        this.platform = platform;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(! platform.isStoreSetup()) {
            sender.sendMessage(ChatColor.RED + "Tebex is not setup yet!");
            return;
        }

        sender.sendMessage("§b[Tebex] §7Performing force check..");
        getPlatform().performCheck();
    }

    @Override
    public String getDescription() {
        return "Rechecks for new purchases.";
    }
}
