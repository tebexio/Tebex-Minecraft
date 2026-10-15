package io.tebex.plugin.command;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import io.tebex.minecraft.commands.Context;
import io.tebex.plugin.VelocityPlatform;
import java.util.*;
import java.util.stream.Collectors;

public final class TebexCommand implements SimpleCommand {
  private final VelocityPlatform platform;

  public TebexCommand(CommandManager manager) {
    platform = manager.getPlatform();
  }

  public void execute(Invocation invocation) {
    String[] args =
        invocation.arguments().length == 0 ? new String[] {"help"} : invocation.arguments();
    Player player = invocation.source() instanceof Player ? (Player) invocation.source() : null;
    Context ctx =
        Context.from(
            player == null,
            player == null ? "" : player.getUsername(),
            player == null ? null : player.getUniqueId(),
            "tebex " + args[0],
            "",
            null,
            args);
    platform
        .runtime()
        .getCommands()
        .process(
            ctx,
            future ->
                future.thenAccept(
                    lines -> {
                      if (!platform.runtime().isStopped())
                        for (String line : lines)
                          invocation.source().sendMessage(legacySection().deserialize(line));
                    }));
  }

  public List<String> suggest(Invocation invocation) {
    String[] args = invocation.arguments();
    if (args.length > 1) return Collections.emptyList();
    String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
    return platform.runtime().getCommands().getCommands().values().stream()
        .filter(
            c ->
                invocation.source().hasPermission(c.getPermission())
                    || invocation.source().hasPermission("tebex.admin"))
        .map(c -> c.getCommandName())
        .filter(name -> name.startsWith(prefix))
        .collect(Collectors.toList());
  }
}
