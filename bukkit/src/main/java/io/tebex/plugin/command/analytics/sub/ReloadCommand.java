package io.tebex.plugin.command.analytics.sub;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.analytics.AnalyticsPlugin;
import io.tebex.analytics.command.SubCommand;
import io.tebex.analytics.sdk.platform.PlatformConfig;
import org.bukkit.command.CommandSender;

import java.io.IOException;

public class ReloadCommand extends SubCommand {
    public ReloadCommand(AnalyticsPlugin platform) {
        super(platform, "reload", "analytics.reload");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        AnalyticsPlugin platform = getPlatform();
        try {
            YamlDocument configYaml = platform.initPlatformConfig();
            PlatformConfig config = platform.loadPlatformConfig(configYaml);

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
}
