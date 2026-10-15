package io.tebex.plugin.command;

import com.google.common.collect.ImmutableList;
import io.tebex.minecraft.commands.Context;
import io.tebex.minecraft.commands.Responder;
import io.tebex.plugin.FoliaPlatform;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public class TebexCommandExecutor implements TabExecutor {
  private final FoliaPlatform platform;

  public TebexCommandExecutor(FoliaPlatform platform) {
    this.platform = platform;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    // Identify sender information
    String senderName = sender.getName();
    UUID senderUUID = null;
    if (sender instanceof Player) {
      senderUUID = ((Player) sender).getUniqueId();
    }

    // Check for console
    boolean isConsole = !(sender instanceof Player);

    // Identify any player targets
    String targetName = "";
    UUID targetUUID = new UUID(0L, 0L);

    // Build the full command and list of arguments, checking if any of the args is a player.
    StringBuilder fullCommand = new StringBuilder(command.getName());
    for (String arg : args) {
      // Assign the target player if we find their username in the args list
      if (targetName.isEmpty()) {
        Player targetPlayer = platform.getPlayer(arg);
        if (targetPlayer != null) {
          targetName = targetPlayer.getName();
          targetUUID = targetPlayer.getUniqueId();
        }
      }

      // Build the full command by appending the arg to the base command
      fullCommand.append(" ").append(arg);
    }

    // Build the command context with the information we have so far for responding.
    Context context =
        Context.from(
            isConsole,
            senderName,
            senderUUID,
            fullCommand.toString(),
            targetName,
            targetUUID,
            args);

    // Show splash for no args /tebex command
    if (args.length == 0 && sender.hasPermission("tebex.tebex")) {
      sender.sendMessage(
          new String[] {
            Responder.formatFancy(context, "Welcome to Tebex!"),
            Responder.formatFancy(
                context,
                "This server is running version {0}",
                "v" + platform.runtime().getPluginVersion())
          });
      return true;
    }

    // Pass context to the command handler, but respond via command sender so it's sent through the
    // appropriate
    // channels (RCON, Console, Player).
    return platform
        .runtime()
        .getCommands()
        .process(
            context,
            (future) -> {
              future.thenAccept(
                  lines -> {
                    Runnable reply =
                        () -> {
                          if (!platform.runtime().isStopped()) sender.sendMessage(lines);
                        };
                    if (sender instanceof Player)
                      platform
                          .runtime()
                          .executeForPlayer(((Player) sender).getUniqueId(), reply, () -> {});
                    else platform.runtime().executeBlocking(reply);
                  });
            });
  }

  @Override
  public List<String> onTabComplete(
      CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 1) {
      return (sender instanceof Player
              ? platform.runtime().getCommands().getAllowedCommands(sender.getName())
              : platform.runtime().getCommands().getCommands())
          .keySet().stream().filter(s -> s.startsWith(args[0])).collect(Collectors.toList());
    }

    return ImmutableList.of();
  }
}
