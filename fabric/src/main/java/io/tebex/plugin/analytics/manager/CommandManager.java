package io.tebex.plugin.analytics.manager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.analytics.command.sub.HelpCommand;
import io.tebex.plugin.analytics.command.sub.SecretCommand;
import io.tebex.plugin.analytics.command.sub.TrackCommand;
import io.tebex.plugin.obj.SubCommand;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

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
                new HelpCommand(platform, this),
                new TrackCommand(platform),
                new SecretCommand(platform)
        ).forEach(command -> commands.put(command.getName(), command));

        LiteralArgumentBuilder<ServerCommandSource> baseCommand = literal("analytics").executes(context -> {
            final ServerCommandSource source = context.getSource();
            platform.sendMessage(source, "Welcome to Tebex Analytics!");
            platform.sendMessage(source, "This server is running version &fv" + platform.getVersion() + "&7.");

            return 1;
        });

        commands.values().forEach(command -> {
            LiteralArgumentBuilder<ServerCommandSource> subCommand = literal(command.getName());

            if(command.getName().equalsIgnoreCase("secret")) {
                baseCommand.then(subCommand.then(argument("key", StringArgumentType.string()).executes(context -> {
                    command.execute(context);
                    return 1;
                })));

                return;
            }

            if(command.getName().equalsIgnoreCase("track")) {
                baseCommand.then(subCommand.then(
                        argument("player", EntityArgumentType.player())
                                .then(argument("event", StringArgumentType.string())
                                        .executes(context -> {
                                            command.execute(context);
                                            return 1;
                                        })
                                        .then(argument("metadata", StringArgumentType.greedyString())
                                                .executes(context -> {
                                                    command.execute(context);
                                                    return 1;
                                                })
                                        )
                                )
                ));

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
