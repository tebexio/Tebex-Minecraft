package io.tebex.plugin.command;

import com.mojang.brigadier.context.CommandContext;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.gui.BuyGUI;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class BuyCommand {
    private final TebexPlugin plugin;
    public BuyCommand(TebexPlugin plugin) {
        this.plugin = plugin;
    }

    public int execute(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource source = context.getSource();

        try {
            ServerPlayerEntity player = source.getPlayer();
            new BuyGUI(plugin).open(player);
        } catch (Exception e) {
            source.sendMessage(Text.of("§b[Tebex] §7You must be a player to run this command!"));
        }

        return 1;
    }
}
