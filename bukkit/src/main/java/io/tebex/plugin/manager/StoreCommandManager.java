package io.tebex.plugin.manager;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.plugin.command.store.TebexCommand;
import io.tebex.plugin.command.store.sub.*;
import org.bukkit.command.PluginCommand;

import java.util.Map;

public class StoreCommandManager {
    private final TebexPlugin platform;
    private final Map<String, SubCommand> commands;

    public StoreCommandManager(TebexPlugin platform) {
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

        TebexCommand tebexCommand = new TebexCommand(this);
        PluginCommand pluginCommand = platform.getCommand("tebex");

        if(pluginCommand == null) {
            throw new RuntimeException("Tebex command not found.");
        }

        pluginCommand.setExecutor(tebexCommand);
        pluginCommand.setTabCompleter(tebexCommand);
    }

    public Map<String, SubCommand> getCommands() {
        return commands;
    }

    public TebexPlugin getPlatform() {
        return platform;
    }
}
