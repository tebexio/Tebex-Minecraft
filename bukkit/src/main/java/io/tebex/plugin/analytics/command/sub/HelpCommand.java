package io.tebex.plugin.analytics.command.sub;

import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.analytics.manager.CommandManager;
import io.tebex.plugin.obj.SubCommand;
import org.bukkit.command.CommandSender;

import java.util.Comparator;

public class HelpCommand extends SubCommand {
    private final CommandManager commandManager;
    public HelpCommand(TebexPlugin platform, CommandManager commandManager) {
        super(platform, "help", "analytics.help");
        this.commandManager = commandManager;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
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
