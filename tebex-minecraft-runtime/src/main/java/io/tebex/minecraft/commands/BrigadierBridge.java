package io.tebex.minecraft.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import io.tebex.minecraft.runtime.TebexRuntime;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** Loader-independent command tree; native modules only supply sender and reply conversions. */
public final class BrigadierBridge<S> {
  private final TebexRuntime platform;
  private final Function<S, Sender> sender;
  private final BiConsumer<S, String[]> reply;

  public BrigadierBridge(
      TebexRuntime platform, Function<S, Sender> sender, BiConsumer<S, String[]> reply) {
    this.platform = platform;
    this.sender = sender;
    this.reply = reply;
  }

  public void register(CommandDispatcher<S> dispatcher) {
    LiteralArgumentBuilder<S> root =
        LiteralArgumentBuilder.<S>literal("tebex").executes(ctx -> welcome(ctx.getSource()));
    platform
        .getCommands()
        .getCommands()
        .forEach(
            (name, command) -> {
              root.then(
                  LiteralArgumentBuilder.<S>literal(name)
                      .requires(
                          source -> {
                            Sender identity = sender.apply(source);
                            return identity.console
                                || platform.hasPermission(identity.name, command.getPermission())
                                || platform.hasPermission(identity.name, "tebex.admin");
                          })
                      .executes(ctx -> run(ctx.getSource(), name))
                      .then(
                          RequiredArgumentBuilder.<S, String>argument(
                                  "arguments", StringArgumentType.greedyString())
                              .executes(
                                  ctx ->
                                      run(
                                          ctx.getSource(),
                                          name
                                              + " "
                                              + StringArgumentType.getString(ctx, "arguments")))));
            });
    dispatcher.register(root);
  }

  private int welcome(S source) {
    Sender identity = sender.apply(source);
    String[] lines;
    if (identity.console || platform.hasPermission(identity.name, "tebex.tebex")) {
      Context context =
          Context.from(
              identity.console, identity.name, identity.uuid, "tebex", "", null, new String[0]);
      lines = Responder.formatWelcome(context, platform.getPluginVersion());
    } else {
      lines = new String[] {"Command not found."};
    }
    platform.executeForPlayer(
        identity.name,
        () -> {
          if (!platform.isStopped()) reply.accept(source, lines);
        },
        () -> {});
    return 1;
  }

  private int run(S source, String input) {
    Sender identity = sender.apply(source);
    String[] tokens = input.split("\\s+");
    Context context =
        Context.from(
            identity.console, identity.name, identity.uuid, "tebex " + tokens[0], "", null, tokens);
    platform
        .getCommands()
        .process(
            context,
            future ->
                future.thenAccept(
                    lines ->
                        platform.executeForPlayer(
                            identity.name,
                            () -> {
                              if (!platform.isStopped()) reply.accept(source, lines);
                            },
                            () -> {})));
    return 1;
  }

  public static final class Sender {
    public final boolean console;
    public final String name;
    public final java.util.UUID uuid;

    public Sender(boolean console, String name, java.util.UUID uuid) {
      this.console = console;
      this.name = name;
      this.uuid = uuid;
    }
  }
}
