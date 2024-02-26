package io.tebex.plugin.store.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.tebex.plugin.TebexPlugin;
import io.tebex.sdk.platform.PlatformLang;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class BuyCommand {
    private final TebexPlugin plugin;
    public BuyCommand(TebexPlugin plugin) {
        this.plugin = plugin;
    }

    public int execute(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource source = context.getSource();

        if(source.getEntity() == null) {
            plugin.sendMessage(source, PlatformLang.MUST_BE_PLAYER.get());
            return 1;
        }

        try {
            ServerPlayerEntity player = source.getPlayer();
            plugin.getBuyGUI().open(player);
        } catch (CommandSyntaxException e) {
            plugin.sendMessage(source, PlatformLang.ERROR_OCCURRED.get(e.getMessage()));
        }

        return 1;
    }
}
