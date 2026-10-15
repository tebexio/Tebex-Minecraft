package io.tebex.plugin.command;

import io.tebex.plugin.BukkitPlatform;
import io.tebex.plugin.gui.BuyGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BuyCommand extends Command {
  private final BukkitPlatform platform;

  public BuyCommand(String command, BukkitPlatform platform) {
    super(command);
    this.platform = platform;
  }

  @Override
  public boolean execute(CommandSender sender, String label, String[] args) {
    String denied =
        platform.runtime().buyAccessError(sender instanceof Player ? sender.getName() : null);
    if (denied != null) {
      sender.sendMessage(ChatColor.RED + denied);
      return true;
    }

    new BuyGUI(platform).open((Player) sender);
    return true;
  }
}
