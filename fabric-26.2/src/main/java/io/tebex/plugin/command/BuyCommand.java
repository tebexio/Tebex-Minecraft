package io.tebex.plugin.command;

import com.mojang.brigadier.context.CommandContext;
import io.tebex.plugin.FabricPluginPlatform;
import io.tebex.plugin.gui.BuyGUI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class BuyCommand {
    private final FabricPluginPlatform platform;
    public BuyCommand(FabricPluginPlatform platform) {
        this.platform = platform;
    }

    public int execute(CommandContext<CommandSourceStack> context) {
        final CommandSourceStack source = context.getSource();

        if (!platform.isSetup()) {
            source.sendSystemMessage(Component.nullToEmpty("§cTebex is not setup yet!"));
            return 1;
        }

        try {
            ServerPlayer player = source.getPlayer();
            new BuyGUI(platform).open(player);
        } catch (Exception e) {
            e.printStackTrace();
            source.sendSystemMessage(Component.nullToEmpty("§b[Tebex] §7You must be a player to run this command!"));
        }

        return 1;
    }
}
