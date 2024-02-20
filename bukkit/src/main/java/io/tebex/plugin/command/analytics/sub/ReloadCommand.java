package io.tebex.plugin.command.analytics.sub;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import org.bukkit.command.CommandSender;

import java.io.IOException;

public class ReloadCommand extends SubCommand {
    public ReloadCommand(TebexPlugin platform) {
        super(platform, "reload", "analytics.reload");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TebexPlugin platform = getPlatform();
        try {
            YamlDocument configYaml = platform.initPlatformConfig();
            ServerPlatformConfig config = platform.loadPlatformConfig(configYaml);

            if(config.hasProxyModeEnabled()) {
                platform.getProxyMessageListener().register();
            } else if(getPlatform().isProxyModeEnabled()) {
                platform.getProxyMessageListener().unregister();
            }

            platform.setProxyModeEnabled(config.hasProxyModeEnabled());
            getPlatform().sendMessage(sender, "The plugin has been reloaded.");
        } catch (IOException e) {
            getPlatform().sendMessage(sender, "&cFailed to reload the plugin. Check console for more information.");
            e.printStackTrace();
        }
    }

    @Override
    public String getDescription() {
        return null;
    }
}
