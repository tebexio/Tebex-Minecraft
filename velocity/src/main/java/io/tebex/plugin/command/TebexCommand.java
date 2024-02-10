package io.tebex.plugin.command;

import com.google.common.collect.ImmutableList;
import io.tebex.plugin.manager.CommandManager;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TebexCommand implements SimpleCommand {
    private CommandManager commandManager;

    public TebexCommand(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

        if(args.length == 0) {
            sender.sendMessage(Component.text("[Tebex] ").color(NamedTextColor.DARK_GRAY).append(Component.text("Welcome to Tebex!").color(NamedTextColor.GRAY)));
            sender.sendMessage(Component.text("[Tebex] ").color(NamedTextColor.DARK_GRAY).append(Component.text("This server is running version v").color(NamedTextColor.GRAY)).append(Component.text(commandManager.getPlatform().getVersion()).color(NamedTextColor.WHITE)).append(Component.text(".").color(NamedTextColor.GRAY)));
            return;
        }

        Map<String, SubCommand> commands = commandManager.getCommands();
        if(! commands.containsKey(args[0].toLowerCase())) {
            sender.sendMessage(Component.text("[Tebex] ").color(NamedTextColor.DARK_GRAY).append(Component.text("Unknown command.").color(NamedTextColor.GRAY)));
            return;
        }

        final SubCommand subCommand = commands.get(args[0].toLowerCase());
        if (! sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(Component.text("[Tebex] ").color(NamedTextColor.AQUA).append(Component.text("You do not have access to that command.").color(NamedTextColor.GRAY)));
            return;
        }

        subCommand.execute(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if(args.length == 1) {
            return commandManager.getCommands()
                    .keySet()
                    .stream()
                    .filter(s -> s.startsWith(args[0]))
                    .collect(Collectors.toList());
        }

        return ImmutableList.of();
    }
}