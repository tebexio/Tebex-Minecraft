package io.tebex.plugin.analytics.manager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.analytics.command.BaseCommand;
import io.tebex.plugin.analytics.command.sub.HelpCommand;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.plugin.analytics.command.sub.SecretCommand;
import io.tebex.plugin.analytics.command.sub.TrackCommand;
import org.bukkit.command.PluginCommand;

import java.util.Map;

public class CommandManager {
    private final TebexPlugin platform;
    private final Map<String, SubCommand> commands;

    public CommandManager(TebexPlugin platform) {
        this.platform = platform;
        this.commands = Maps.newHashMap();
    }

    public void register() {
        ImmutableList.of(
                new SecretCommand(platform),
                new TrackCommand(platform),
                new HelpCommand(platform, this)
        ).forEach(command -> commands.put(command.getName(), command));

        BaseCommand baseCommand = new BaseCommand(this);
        PluginCommand pluginCommand = platform.getCommand("analytics");

        if(pluginCommand == null) {
            throw new RuntimeException("Analytics command not found.");
        }

        pluginCommand.setExecutor(baseCommand);
        pluginCommand.setTabCompleter(baseCommand);
    }

    public Map<String, SubCommand> getCommands() {
        return commands;
    }

    public TebexPlugin getPlatform() {
        return platform;
    }
}
