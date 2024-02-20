package io.tebex.plugin.manager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.plugin.command.analytics.AnalyticsCommand;
import io.tebex.plugin.command.analytics.sub.SetupCommand;
import io.tebex.plugin.command.analytics.sub.TrackCommand;
import org.bukkit.command.PluginCommand;

import java.util.Map;

public class AnalyticsCommandManager {
    private final TebexPlugin platform;
    private final Map<String, SubCommand> commands;

    public AnalyticsCommandManager(TebexPlugin platform) {
        this.platform = platform;
        this.commands = Maps.newHashMap();
    }

    public void register() {
        ImmutableList.of(
                new SetupCommand(platform),
                new TrackCommand(platform)
        ).forEach(command -> {
            commands.put(command.getName(), command);
        });

        AnalyticsCommand analyticsCommand = new AnalyticsCommand(this);
        PluginCommand pluginCommand = platform.getCommand("analytics");

        if(pluginCommand == null) {
            throw new RuntimeException("Analytics command not found.");
        }

        pluginCommand.setExecutor(analyticsCommand);
        pluginCommand.setTabCompleter(analyticsCommand);
    }

    public Map<String, SubCommand> getCommands() {
        return commands;
    }

    public TebexPlugin getPlatform() {
        return platform;
    }
}
