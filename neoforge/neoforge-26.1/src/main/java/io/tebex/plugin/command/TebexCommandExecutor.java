package io.tebex.plugin.command;

import static net.minecraft.commands.Commands.literal;

import com.mojang.brigadier.CommandDispatcher;
import io.tebex.minecraft.commands.BrigadierBridge;
import io.tebex.minecraft.runtime.TebexRuntime;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/** Native source conversion and buy registration; the command tree is shared. */
public final class TebexCommandExecutor {
  private final TebexRuntime platform;

  public TebexCommandExecutor(TebexRuntime platform) {
    this.platform = platform;
  }

  public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
    io.tebex.minecraft.platform.config.ServerPlatformConfig config = platform.getConfig();
    if (config.isBuyCommandEnabled())
      dispatcher.register(
          literal(config.getBuyCommandName()).executes(new BuyCommand(platform)::execute));
    new BrigadierBridge<CommandSourceStack>(
            platform,
            source ->
                new BrigadierBridge.Sender(
                    source.getEntity() == null,
                    source.getTextName(),
                    source.getEntity() == null ? null : source.getEntity().getUUID()),
            (source, lines) -> {
              for (String line : lines) source.sendSystemMessage(Component.literal(line));
            })
        .register(dispatcher);
  }
}
