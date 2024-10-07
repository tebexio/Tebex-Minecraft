package io.tebex.plugin.command.sub;

import com.mojang.brigadier.context.CommandContext;
import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.command.SubCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.io.IOException;

public class ReloadCommand extends SubCommand {
    public ReloadCommand(TebexPlugin platform) {
        super(platform, "reload", "tebex.reload");
    }

    @Override
    public void execute(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource source = context.getSource();

        TebexPlugin platform = getPlatform();
        try {
            YamlDocument configYaml = platform.initPlatformConfig();
            platform.loadServerPlatformConfig(configYaml);
            platform.refreshListings();
//            platform.setBuyGUI(new BuyGUI(platform));
            platform.getSDK().sendPluginEvents();

            source.sendMessage(Text.of("§8[Tebex] §7Successfully reloaded."));
        } catch (IOException e) {
            source.sendMessage(Text.of("§8[Tebex] §cFailed to reload the plugin: Check Console."));
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getDescription() {
        return "Reloads the plugin.";
    }
}
