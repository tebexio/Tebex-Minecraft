package io.tebex.plugin.command.sub;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import org.bukkit.command.CommandSender;

public class ReportCommand extends SubCommand {
    public ReportCommand(TebexPlugin platform) {
        super(platform, "report", "tebex.report");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TebexPlugin platform = getPlatform();

        if (args.length != 1) {
            platform.sendMessage(sender, "&cInvalid command usage. Use /tebex " + this.getName() + " " + getUsage());
            return;
        }

        String message = String.join(" ", args);
        if (message.isEmpty()) {
            platform.sendMessage(sender, "&cA message is required for your report.");
            return;
        }

        platform.sendMessage(sender, "Sending your report to Tebex...");
        platform.sendTriageEvent(new Error("User reported error in-game: " + message));
    }

    @Override
    public String getDescription() {
        return "Reports a problem to Tebex along with information about your webstore, server, etc.";
    }

    @Override
    public String getUsage() {
        return "<message>";
    }
}
