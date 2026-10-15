package io.tebex.minecraft.commands;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import lombok.Getter;

/** A PlayerCommand represents a command that can be run by a player. */
@Getter
public class PlayerCommand {
  /** The base name of the Tebex command (info, help, forcecheck, etc.) */
  private final String commandName;

  /**
   * Usage should be space-separated values surrounded by <angle brackets>. These are parsed into
   * args required
   */
  private final String usage;

  private final int numArgsRequired;

  /** A description of the command used for /help */
  private final String description;

  /** The permission for this command */
  private final String permission;

  /** The actual function this command performs */
  private final Function<Context, CompletableFuture<String[]>> handler;

  PlayerCommand(
      String commandName,
      String usage,
      String description,
      Function<Context, CompletableFuture<String[]>> handler) {
    this.commandName = commandName;
    this.usage = usage;
    this.description = description;
    this.handler = handler;
    this.permission = TebexCommands.TEBEX_COMMAND_PREFIX + "." + commandName;

    // Number of arguments are occurrences of opening '<' tags in the usage string
    this.numArgsRequired =
        (int)
            java.util.Arrays.stream(usage.split(" "))
                .filter(arg -> arg.startsWith("<") && !arg.startsWith("<opt:"))
                .count();
  }
}
