package io.tebex.plugin.analytics.command;

import com.google.common.collect.ImmutableList;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.sdk.platform.PlatformLang;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BaseCommand implements TabExecutor {
    private final CommandManager commandManager;

    public BaseCommand(CommandManager commandManager) {
        this.commandManager = commandManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 0 && sender.hasPermission("analytics.admin")) {
            commandManager.getPlatform().sendMessage(sender, "Welcome to Tebex Analytics!");
            commandManager.getPlatform().sendMessage(sender, "This server is running version &fv" + commandManager.getPlatform().getDescription().getVersion() + "&7.");
            return true;
        } else if(args.length == 0) {
            commandManager.getPlatform().sendMessage(sender, PlatformLang.NO_PERMISSION.get());
            return true;
        }

        Map<String, SubCommand> commands = commandManager.getCommands();
        if(! commands.containsKey(args[0].toLowerCase())) {
            commandManager.getPlatform().sendMessage(sender, PlatformLang.UNKNOWN_COMMAND.get());
            return true;
        }

        final SubCommand subCommand = commands.get(args[0].toLowerCase());
        if (! sender.hasPermission(subCommand.getPermission())) {
            commandManager.getPlatform().sendMessage(sender, PlatformLang.NO_PERMISSION.get());
            return true;
        }

        String[] commandArgs = Arrays.copyOfRange(args, 1, args.length);
        if(commandArgs.length < subCommand.getMinArgs()) {
            commandManager.getPlatform().sendMessage(sender, PlatformLang.INVALID_USAGE.get("analytics", subCommand.getName() + " " + subCommand.getUsage()));
            return true;
        }

        subCommand.execute(sender, commandArgs);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(args.length == 1) {
            return commandManager.getCommands()
                    .keySet()
                    .stream()
                    .filter(s -> s.startsWith(args[0]))
                    .filter(s -> sender.hasPermission(commandManager.getCommands().get(s).getPermission()))
                    .collect(Collectors.toList());
        }

        return ImmutableList.of();
    }
}
