package io.tebex.plugin.store.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.plugin.store.command.sub.*;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;
import java.util.Map;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CommandManager {
    private final TebexPlugin platform;
    private final Map<String, SubCommand> commands;

    public CommandManager(TebexPlugin platform) {
        this.platform = platform;
        this.commands = Maps.newHashMap();
    }

    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        ImmutableList.of(
                new SecretCommand(platform),
                new ReloadCommand(platform),
                new ForceCheckCommand(platform),
                new HelpCommand(platform, this),
                new BanCommand(platform),
                new CheckoutCommand(platform),
                new DebugCommand(platform),
                new InfoCommand(platform),
                new LookupCommand(platform),
                new ReportCommand(platform),
                new SendLinkCommand(platform),
                new GoalsCommand(platform)
        ).forEach(command -> commands.put(command.getName(), command));

        LiteralArgumentBuilder<ServerCommandSource> baseCommand = literal("tebex").executes(context -> {
            final ServerCommandSource source = context.getSource();
            platform.sendMessage(source, "Welcome to Tebex!");
            platform.sendMessage(source, "This server is running version &fv" + platform.getVersion() + "&7.");

            return 1;
        });

        BuyCommand buyCommand = new BuyCommand(platform);
        dispatcher.register(literal(platform.getPlatformConfig().getBuyCommandName()).executes(buyCommand::execute));

        commands.values().forEach(command -> {
            LiteralArgumentBuilder<ServerCommandSource> subCommand = literal(command.getName());

            if(command.getName().equalsIgnoreCase("secret")) {
                baseCommand.then(subCommand.then(argument("key", StringArgumentType.string()).executes(context -> {
                    command.execute(context);
                    return 1;
                })));

                return;
            }

            if(command.getName().equalsIgnoreCase("debug")) {
                baseCommand.then(subCommand.then(argument("trueOrFalse", StringArgumentType.string()).executes(context -> {
                    command.execute(context);
                    return 1;
                })));

                return;
            }

            baseCommand.then(subCommand.executes(context -> {
                command.execute(context);
                return 1;
            }));
        });

        dispatcher.register(baseCommand);
    }

    public Map<String, SubCommand> getCommands() {
        return commands;
    }

    public TebexPlugin getPlatform() {
        return platform;
    }
}
