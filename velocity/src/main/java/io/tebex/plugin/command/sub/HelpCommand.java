package io.tebex.plugin.command.sub;

import com.velocitypowered.api.command.CommandSource;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.plugin.manager.CommandManager;

import java.util.Comparator;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

public class HelpCommand extends SubCommand {
    private final CommandManager commandManager;

    public HelpCommand(TebexPlugin platform, CommandManager commandManager) {
        super(platform, "help", "tebex.admin");
        this.commandManager = commandManager;
    }

    @Override
    public void execute(CommandSource sender, String[] args) {
        sender.sendMessage(legacySection().deserialize("§b[Tebex] §7Plugin Commands:"));

        commandManager
                .getCommands()
                .values()
                .stream()
                .sorted(Comparator.comparing(SubCommand::getName))
                .forEach(subCommand -> sender.sendMessage(legacySection().deserialize(" §8- §f/tebex " + subCommand.getName() + "§f" + (!subCommand.getUsage().isEmpty() ? " §3" + subCommand.getUsage() + " " : " ") + "§7§o(" + subCommand.getDescription() + ")")));
    }

    @Override
    public String getDescription() {
        return "Shows this help page.";
    }
}
