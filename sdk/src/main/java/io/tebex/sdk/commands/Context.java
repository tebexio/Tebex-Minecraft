package io.tebex.sdk.commands;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Context provides information about a PlayerCommand to be executed based on the sender and the provided input.
 */
@Getter public class Context {
    private final boolean fromConsole;
    private final String senderUsername;
    private final UUID senderUUID;
    private final String fullCommand;
    private final String commandName;
    private final String[] arguments;
    private final String targetUsername;
    private final UUID targetUUID;

    private Context(boolean fromConsole, String senderUsername, UUID senderUUID, String targetUsername, UUID targetUUID, String command, String[] arguments){
        this.fromConsole = fromConsole;
        this.senderUsername = senderUsername == null ? "" : senderUsername;
        this.senderUUID = senderUUID;
        this.targetUsername = targetUsername == null ? "" : targetUsername;
        this.targetUUID = targetUUID;
        this.fullCommand = command;

        // Split out the prefix from the actual command so we don't count the subcommand as an argument
        command = command.replaceAll(TebexCommands.TEBEX_COMMAND_PREFIX + " ", "");
        arguments = parseQuotedArguments(arguments);
        if (arguments.length > 0) {
            this.commandName = command.split(" ")[0];
            this.arguments = Arrays.copyOfRange(arguments, 1, arguments.length);
        } else {
            this.commandName = command;
            this.arguments = new String[0];
        }
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

    public static Context from(boolean isConsole, String senderUsername, UUID senderUUID, String fullCommand, String targetUsername, UUID targetUUID, String[] allArguments) {
        return new Context(isConsole, senderUsername, senderUUID, targetUsername, targetUUID, fullCommand, allArguments);
    }

    public void tellTarget(String message) {
        TebexCommands.getPlatform().sendPlayerMessage(targetUsername, message);
    }
}
