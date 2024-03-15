package io.tebex.plugin.store.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.plugin.store.command.sub.*;
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

        BaseCommand baseCommand = new BaseCommand(this);
        PluginCommand pluginCommand = platform.getCommand("tebex");

        if(pluginCommand == null) {
            throw new RuntimeException("Tebex command not found.");
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
