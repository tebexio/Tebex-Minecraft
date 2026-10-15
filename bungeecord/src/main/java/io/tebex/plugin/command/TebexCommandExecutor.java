package io.tebex.plugin.command;

import io.tebex.minecraft.commands.Context;
import io.tebex.plugin.BungeePlatform;
import java.util.*;
import java.util.stream.Collectors;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

/** Bungee input/reply adapter for the shared registry. */
public final class TebexCommandExecutor extends Command implements TabExecutor {
  private final BungeePlatform platform;

  public TebexCommandExecutor(BungeePlatform platform) {
    super("tebex");
    this.platform = platform;
  }

  @Override
  public void execute(CommandSender sender, String[] input) {
    String[] args = input.length == 0 ? new String[] {"help"} : input;
    ProxiedPlayer player = sender instanceof ProxiedPlayer ? (ProxiedPlayer) sender : null;
    Context context =
        Context.from(
            player == null,
            sender.getName(),
            player == null ? null : player.getUniqueId(),
            "tebex " + args[0],
            "",
            null,
            args);
    platform
        .runtime()
        .getCommands()
        .process(
            context,
            future ->
                future.thenAccept(
                    lines -> {
                      if (!platform.runtime().isStopped())
                        for (String line : lines)
                          sender.sendMessage(TextComponent.fromLegacyText(line));
                    }));
  }

  @Override
  public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
    if (args.length > 1) return Collections.emptyList();
    String prefix = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
    return platform.runtime().getCommands().getCommands().values().stream()
        .filter(
            command ->
                !(sender instanceof ProxiedPlayer)
                    || sender.hasPermission(command.getPermission())
                    || sender.hasPermission("tebex.admin"))
        .map(command -> command.getCommandName())
        .filter(name -> name.startsWith(prefix))
        .collect(Collectors.toList());
  }
}
