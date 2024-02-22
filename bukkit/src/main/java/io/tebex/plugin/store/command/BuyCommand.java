package io.tebex.plugin.store.command;

import io.tebex.plugin.TebexPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BuyCommand extends Command {
    private final TebexPlugin platform;

    public BuyCommand(String command, TebexPlugin platform) {
        super(command);
        this.platform = platform;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if(! platform.isStoreSetup()) {
            platform.sendMessage(sender, "&cTebex is not setup yet!");
            return true;
        }

        if(! (sender instanceof Player)) {
            platform.sendMessage(sender, "&cOnly players can use this command!");
            return true;
        }

        Player player = (Player) sender;
        platform.getStoreManager().getBuyGui().open(player);

        return true;
    }
}
