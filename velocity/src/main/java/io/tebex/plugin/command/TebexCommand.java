package io.tebex.plugin.command;

import com.google.common.collect.ImmutableList;
import io.tebex.plugin.manager.CommandManager;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

public class TebexCommand implements SimpleCommand {
    private final CommandManager commandManager;

    public TebexCommand(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource sender = invocation.source();
        String[] args = invocation.arguments();

        if(args.length == 0) {
            sender.sendMessage(legacySection().deserialize("§8[Tebex] §7Welcome to Tebex!"));
            sender.sendMessage(legacySection().deserialize("§8[Tebex] §7This server is running version §fv" + commandManager.getPlatform().getPluginVersion() + "§7."));
            return;
        }

        Map<String, SubCommand> commands = commandManager.getCommands();
        if(! commands.containsKey(args[0].toLowerCase())) {
            sender.sendMessage(legacySection().deserialize("§8[Tebex] §7Unknown command."));
            return;
        }

        final SubCommand subCommand = commands.get(args[0].toLowerCase());
        if (! sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(legacySection().deserialize("§b[Tebex] §7You do not have access to that command."));
            return;
        }

        subCommand.execute(sender, parseQuotedArguments(Arrays.copyOfRange(args, 1, args.length)));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if(args.length <= 1) {
            return this.suggestions(invocation.source());
        }

        return ImmutableList.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return !this.suggestions(invocation.source()).isEmpty();
    }

    private List<String> suggestions(CommandSource source) {
        return commandManager.getCommands()
            .values()
            .stream()
            .filter(command -> source.hasPermission(command.getPermission()))
            .map(SubCommand::getName)
            .collect(Collectors.toList());
    }

    private String[] parseQuotedArguments(String[] arguments) {
        List<String> parsedArguments = new ArrayList<>();
        StringBuilder quotedArgument = new StringBuilder();
        boolean inQuote = false;

        for (String argument : arguments) {
            if (!inQuote && argument.startsWith("\"")) {
                inQuote = true;
                quotedArgument.append(argument.substring(1));
                if (argument.endsWith("\"") && argument.length() > 1) {
                    inQuote = false;
                    quotedArgument.setLength(quotedArgument.length() - 1);
                    parsedArguments.add(quotedArgument.toString());
                    quotedArgument.setLength(0);
                }
                continue;
            }

            if (inQuote) {
                quotedArgument.append(" ").append(argument);
                if (argument.endsWith("\"")) {
                    inQuote = false;
                    quotedArgument.setLength(quotedArgument.length() - 1);
                    parsedArguments.add(quotedArgument.toString());
                    quotedArgument.setLength(0);
                }
                continue;
            }

            parsedArguments.add(argument);
        }

        if (inQuote) {
            parsedArguments.add(quotedArgument.toString());
        }

        return parsedArguments.toArray(new String[0]);
    }
}
