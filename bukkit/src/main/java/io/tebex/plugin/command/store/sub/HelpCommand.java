package io.tebex.plugin.command.sub;

import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.plugin.manager.StoreCommandManager;
import org.bukkit.command.CommandSender;

import java.util.Comparator;

public class HelpCommand extends SubCommand {
    private final StoreCommandManager commandManager;
    public HelpCommand(TebexPlugin platform, StoreCommandManager commandManager) {
        super(platform, "help", "tebex.admin");
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
                .forEach(subCommand -> getPlatform().sendMessage(sender, " &8- &f/tebex " + subCommand.getName() + "&f" + (!subCommand.getUsage().isEmpty() ? " &3" + subCommand.getUsage() + " " : " ") + "&7&o(" + subCommand.getDescription() + ")"));
    }

    @Override
    public String getDescription() {
        return "Shows this help page.";
    }
}
