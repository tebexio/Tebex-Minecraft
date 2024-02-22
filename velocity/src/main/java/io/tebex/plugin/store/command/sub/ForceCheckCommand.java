package io.tebex.plugin.command.sub;

import com.velocitypowered.api.command.CommandSource;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import net.kyori.adventure.text.Component;

public class ForceCheckCommand extends SubCommand {
    private final TebexPlugin platform;

    public ForceCheckCommand(TebexPlugin platform) {
        super(platform, "forcecheck", "tebex.forcecheck");
        this.platform = platform;
    }

    @Override
    public void execute(CommandSource sender, String[] args) {
        if(! platform.isStoreSetup()) {
            sender.sendMessage(Component.text("§cTebex is not setup yet!"));
            return;
        }

        sender.sendMessage(Component.text("§b[Tebex] §7Performing force check..."));
        getPlatform().performCheck(false);
    }

    @Override
    public String getDescription() {
        return "Checks immediately for new purchases.";
    }
}
