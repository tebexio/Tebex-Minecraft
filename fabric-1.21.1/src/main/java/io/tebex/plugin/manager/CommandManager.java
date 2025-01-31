package io.tebex.plugin.manager;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.BuyCommand;
import io.tebex.plugin.command.SubCommand;
import io.tebex.plugin.command.sub.*;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class CommandManager {
    private final TebexPlugin platform;
    private final List<SubCommand> commands;

    public CommandManager(TebexPlugin  platform) {
        this.platform = platform;
        this.commands = ImmutableList.of(
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
        );
    }

    public void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> baseCommand = literal("tebex")
                .requires(source -> TebexPlugin.hasPermission(source, "tebex.base"))
                .executes(context -> {
                    final ServerCommandSource source = context.getSource();
                    source.sendMessage(Text.of("§8[Tebex] §7Welcome to Tebex!"));
                    source.sendMessage(Text.of("§8[Tebex] §7This server is running version §fv" + platform.getVersion() + "§7."));

                    return 1;
                });

        if (platform.getPlatformConfig().isBuyCommandEnabled()) {
            BuyCommand buyCommand = new BuyCommand(platform);
            dispatcher.register(literal(platform.getPlatformConfig().getBuyCommandName()).executes(buyCommand::execute));
        }

        commands.forEach(command -> {
            LiteralArgumentBuilder<ServerCommandSource> subCommand = literal(command.getName())
                    .requires(source -> TebexPlugin.hasPermission(source, command.getPermission()));

            if (command.getName().equalsIgnoreCase("secret")) {
                baseCommand.then(subCommand.then(argument("key", StringArgumentType.string()).executes(context -> {
                    command.execute(context);
                    return 1;
                })));
            }

            else if(command.getName().equalsIgnoreCase("debug")) {
                baseCommand.then(subCommand.then(argument("trueOrFalse", StringArgumentType.string()).executes(context -> {
                    command.execute(context);
                    return 1;
                })));
            }

            else if(command.getName().equalsIgnoreCase("ban")) {
                baseCommand.then(subCommand.then(argument("playerName", StringArgumentType.string()).executes(context -> {
                    command.execute(context);
                    return 1;
                })));
            }

            else if(command.getName().equalsIgnoreCase("checkout")) {
                baseCommand.then(subCommand.then(argument("packageId", StringArgumentType.string()).executes(context -> {
                    command.execute(context);
                    return 1;
                })));
            }

            else if(command.getName().equalsIgnoreCase("lookup")) {
                baseCommand.then(subCommand.then(argument("username", StringArgumentType.string()).executes(context -> {
                    command.execute(context);
                    return 1;
                })));
            }

            else if(command.getName().equalsIgnoreCase("report")) {
                baseCommand.then(subCommand.then(argument("message", StringArgumentType.string()).executes(context -> {
                    command.execute(context);
                    return 1;
                })));
            }

            else if(command.getName().equalsIgnoreCase("sendlink")) {
                baseCommand.then(subCommand
                        .then(argument("username", StringArgumentType.string())
                                .then(argument("packageId", StringArgumentType.string()))
                                .executes(context -> {
                                    command.execute(context);
                                    return 1;
                                })));
            }

            else {
                baseCommand.then(subCommand.executes(context -> {
                    command.execute(context);
                    return 1;
                }));
            }
        });

        dispatcher.register(baseCommand);
    }

    public TebexPlugin getPlatform() {
        return platform;
    }

    public List<SubCommand> getCommands() {
        return commands;
    }
}
