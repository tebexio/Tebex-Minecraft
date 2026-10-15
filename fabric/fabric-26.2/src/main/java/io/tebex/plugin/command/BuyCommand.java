package io.tebex.plugin.command;

import com.mojang.brigadier.context.CommandContext;
import io.tebex.minecraft.runtime.TebexRuntime;
import io.tebex.plugin.gui.BuyGUI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class BuyCommand {
  private final TebexRuntime platform;

  public BuyCommand(TebexRuntime platform) {
    this.platform = platform;
  }

  public int execute(CommandContext<CommandSourceStack> context) {
    CommandSourceStack source = context.getSource();
    ServerPlayer player =
        source.getEntity() instanceof ServerPlayer ? (ServerPlayer) source.getEntity() : null;
    String denied = platform.buyAccessError(player == null ? null : player.getName().getString());
    if (denied != null) {
      source.sendSystemMessage(Component.literal(denied));
      return 0;
    }
    new BuyGUI(platform).open(player);
    return 1;
  }
}
