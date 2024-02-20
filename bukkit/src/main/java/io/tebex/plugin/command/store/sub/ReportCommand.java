package io.tebex.plugin.command.sub;

import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import org.bukkit.command.CommandSender;

public class ReportCommand extends SubCommand {
    public ReportCommand(TebexPlugin platform) {
        super(platform, "report", "tebex.report");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TebexPlugin platform = getPlatform();

        if (args.length == 0) {
            platform.sendMessage(sender, "&cA message is required for your report.");
            return;
        }

        String message = String.join(" ", args);

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
