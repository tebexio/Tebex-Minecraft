package io.tebex.plugin.analytics.command.sub;

import com.mojang.brigadier.context.CommandContext;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.analytics.manager.CommandManager;
import io.tebex.plugin.obj.SubCommand;
import net.minecraft.server.command.ServerCommandSource;

import java.util.Comparator;

public class HelpCommand extends SubCommand {
    private final CommandManager commandManager;
    public HelpCommand(TebexPlugin platform, CommandManager commandManager) {
        super(platform, "help", "tebex.admin");
        this.commandManager = commandManager;
    }

    @Override
    public void execute(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource sender = context.getSource();

        getPlatform().sendMessage(sender, "Plugin Commands:");

        commandManager
                .getCommands()
                .values()
                .stream()
                .sorted(Comparator.comparing(SubCommand::getName))
                .forEach(subCommand -> getPlatform().sendMessage(sender, " &8- &f/analytics " + subCommand.getName() + "&f" + (!subCommand.getUsage().isEmpty() ? " &3" + subCommand.getUsage() + " " : " ") + "&7&o(" + subCommand.getDescription() + ")"));
    }

    @Override
    public String getDescription() {
        return "Shows this help page.";
    }
}
