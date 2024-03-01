package io.tebex.plugin.store.command.sub;

import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.sdk.platform.PlatformLang;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ForceCheckCommand extends SubCommand {
    public ForceCheckCommand(TebexPlugin platform) {
        super(platform, "forcecheck", "tebex.forcecheck");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        final TebexPlugin platform = getPlatform();

        if (! platform.isStoreSetup()) {
            platform.sendMessage(sender, PlatformLang.NOT_CONNECTED_TO_STORE.get());
            return;
        }

        if(sender instanceof Player) {
            platform.sendMessage(sender, "Performing force check...");
        }

        getPlatform().performCheck(false);
    }

    @Override
    public String getDescription() {
        return "Checks immediately for new purchases.";
    }
}
