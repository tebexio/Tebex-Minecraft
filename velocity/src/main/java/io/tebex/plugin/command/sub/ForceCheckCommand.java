package io.tebex.plugin.command.sub;

import com.velocitypowered.api.command.CommandSource;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ForceCheckCommand extends SubCommand {
    private final TebexPlugin platform;

    public ForceCheckCommand(TebexPlugin platform) {
        super(platform, "forcecheck", "tebex.forcecheck");
        this.platform = platform;
    }

    @Override
    public void execute(CommandSource sender, String[] args) {
        if(! platform.isSetup()) {
            sender.sendMessage(Component.text("Tebex is not setup yet!").color(NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("[Tebex] ").color(NamedTextColor.AQUA).append(Component.text("Performing force check...").color(NamedTextColor.GRAY)));
        getPlatform().performCheck(false);
    }

    @Override
    public String getDescription() {
        return "Checks immediately for new purchases.";
    }
}
