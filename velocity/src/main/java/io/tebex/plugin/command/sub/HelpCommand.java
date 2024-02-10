package io.tebex.plugin.command.sub;

import com.velocitypowered.api.command.CommandSource;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.plugin.manager.CommandManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Comparator;

public class HelpCommand extends SubCommand {
    private final CommandManager commandManager;

    public HelpCommand(TebexPlugin platform, CommandManager commandManager) {
        super(platform, "help", "tebex.admin");
        this.commandManager = commandManager;
    }

    @Override
    public void execute(CommandSource sender, String[] args) {
        sender.sendMessage(Component.text("[Tebex] ").color(NamedTextColor.AQUA).append(Component.text("Plugin Commands:").color(NamedTextColor.GRAY)));

        commandManager
                .getCommands()
                .values()
                .stream()
                .sorted(Comparator.comparing(SubCommand::getName))
                .forEach(subCommand -> sender.sendMessage(Component.text(" - ").color(NamedTextColor.DARK_GRAY)
                        .append(Component.text("/tebex " + subCommand.getName()).color(NamedTextColor.WHITE))
                        .append(Component.text(subCommand.getUsage().isEmpty() ? " " : " " + subCommand.getUsage() + " ").color(NamedTextColor.DARK_AQUA))
                        .append(Component.text("(" + subCommand.getDescription() + ")").color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))));
    }

    @Override
    public String getDescription() {
        return "Shows this help page.";
    }
}
