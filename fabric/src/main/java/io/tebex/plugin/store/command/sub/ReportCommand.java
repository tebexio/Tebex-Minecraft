package io.tebex.plugin.store.command.sub;

import com.mojang.brigadier.context.CommandContext;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import net.minecraft.server.command.ServerCommandSource;

public class ReportCommand extends SubCommand {
    public ReportCommand(TebexPlugin platform) {
        super(platform, "report", "tebex.report");
    }

    @Override
    public void execute(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource sender = context.getSource();
        final TebexPlugin platform = getPlatform();

        String message = context.getArgument("message", String.class);

        if (message.isBlank()) {
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
