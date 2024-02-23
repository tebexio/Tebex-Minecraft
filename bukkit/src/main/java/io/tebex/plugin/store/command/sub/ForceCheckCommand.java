package io.tebex.plugin.store.command.sub;

import io.tebex.plugin.util.Lang;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import org.bukkit.command.CommandSender;

public class ForceCheckCommand extends SubCommand {
    private final TebexPlugin platform;

    public ForceCheckCommand(TebexPlugin platform) {
        super(platform, "forcecheck", "tebex.forcecheck");
        this.platform = platform;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if(! platform.isStoreSetup()) {
            platform.sendMessage(sender, Lang.NOT_CONNECTED_TO_STORE.get());
            return;
        }

        platform.sendMessage(sender, "Performing force check...");
        getPlatform().performCheck(false);
    }

    @Override
    public String getDescription() {
        return "Checks immediately for new purchases.";
    }
}
