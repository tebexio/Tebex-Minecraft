package io.tebex.plugin.store.command.sub;

import com.mojang.brigadier.context.CommandContext;
import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.sdk.platform.PlatformLang;
import net.minecraft.server.command.ServerCommandSource;

import java.io.IOException;

public class ReloadCommand extends SubCommand {
    public ReloadCommand(TebexPlugin platform) {
        super(platform, "reload", "tebex.reload");
    }

    @Override
    public void execute(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource sender = context.getSource();
        final TebexPlugin platform = getPlatform();

        try {
            YamlDocument configYaml = platform.initPlatformConfig();
            platform.loadServerPlatformConfig(configYaml);
            platform.refreshListings();

            platform.sendMessage(sender, PlatformLang.RELOAD_SUCCESS.get());
        } catch (IOException e) {
            platform.sendMessage(sender, PlatformLang.RELOAD_FAILURE.get());
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDescription() {
        return "Reloads the plugin.";
    }
}
