package io.tebex.minecraft.commands;

import io.tebex.minecraft.platform.config.ServerPlatformConfig;
import io.tebex.minecraft.runtime.TebexRuntime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/** One registry per runtime, shared by all native command adapters. */
public final class TebexCommands {
  public static final String TEBEX_COMMAND_PREFIX = "tebex";
  private final TebexRuntime platform;
  private final Map<String, PlayerCommand> commands = new LinkedHashMap<>();
  private final Set<String> restricted = new HashSet<>();

  public TebexCommands(TebexRuntime platform) {
    this.platform = platform;
    registerCommands();
  }

  private void register(
      String name,
      String usage,
      String description,
      Function<Context, CompletableFuture<String[]>> handler) {
    commands.put(name, new PlayerCommand(name, usage, description, handler));
  }

  /** Velocity historically permits omitted ban reason and IP. Only metadata differs. */
  public void useOptionalBanArguments() {
    PlayerCommand command = commands.get("ban");
    commands.put(
        "ban",
        new PlayerCommand(
            "ban",
            "<playerName> <opt:reason> <opt:ip>",
            command.getDescription(),
            command.getHandler()));
  }

  public void setRestrictedToCommands(String... names) {
    restricted.clear();
    restricted.addAll(Arrays.asList(names));
  }

  public Map<String, PlayerCommand> getCommands() {
    Map<String, PlayerCommand> exposed = new LinkedHashMap<>();
    commands.forEach(
        (name, command) -> {
          if (restricted.isEmpty() || restricted.contains(name)) exposed.put(name, command);
        });
    return Collections.unmodifiableMap(exposed);
  }

  private boolean allowed(String username, PlayerCommand command) {
    return platform.hasPermission(username, command.getPermission())
        || platform.hasPermission(username, "tebex.admin");
  }

  public Map<String, PlayerCommand> getAllowedCommands(String username) {
    Map<String, PlayerCommand> allowed = new LinkedHashMap<>();
    getCommands()
        .forEach(
            (name, command) -> {
              if (allowed(username, command)) allowed.put(name, command);
            });
    return allowed;
  }

  public boolean process(Context ctx, Consumer<CompletableFuture<String[]>> respond) {
    String name = ctx.getCommandName().replace("tebex ", "").toLowerCase(Locale.ROOT);
    PlayerCommand command = getCommands().get(name);
    if (command == null) {
      respond.accept(lines("Command not found."));
      return false;
    }
    if (!ctx.isFromConsole() && !allowed(ctx.getSenderUsername(), command)) {
      respond.accept(lines("Unrecognized command or no permission."));
      return false;
    }
    if (ctx.getArguments().length < command.getNumArgsRequired()) {
      respond.accept(lines("Usage: /tebex " + name + " " + command.getUsage()));
      return false;
    }
    if (!platform.isSetup() && !Arrays.asList("help", "secret", "reload", "debug").contains(name)) {
      respond.accept(lines("Tebex is not connected. Use /tebex secret <key>."));
      return true;
    }
    try {
      respond.accept(
          command
              .getHandler()
              .apply(ctx)
              .handle(
                  (output, error) ->
                      error == null
                          ? output
                          : new String[] {
                            Responder.formatError(ctx, "Command failed. " + safeMessage(error))
                          }));
    } catch (Exception error) {
      respond.accept(lines("Command failed. " + safeMessage(error)));
    }
    return true;
  }

  private String safeMessage(Throwable error) {
    String message =
        error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
    String secret = platform.getSession() == null ? "" : platform.getSession().getSecretKey();
    return secret.isEmpty() ? message : message.replace(secret, "[redacted]");
  }

  private static CompletableFuture<String[]> lines(String... lines) {
    return CompletableFuture.completedFuture(lines);
  }

  private void registerCommands() {
    register(
        "help",
        "",
        "Shows available commands.",
        ctx -> {
          List<String> out = new ArrayList<>();
          out.add("Available commands:");
          getCommands()
              .forEach(
                  (name, command) -> {
                    if (ctx.isFromConsole() || allowed(ctx.getSenderUsername(), command))
                      out.add(
                          "/tebex "
                              + name
                              + " "
                              + command.getUsage()
                              + " | "
                              + command.getDescription());
                  });
          return lines(out.toArray(new String[0]));
        });
    register(
        "secret",
        "<key>",
        "Connects to your store.",
        ctx ->
            platform
                .connect(ctx.getArguments()[0], true)
                .thenApply(
                    info ->
                        new String[] {
                          "Successfully connected to your store: "
                              + info.getAccount().getName()
                              + " as "
                              + info.getServer().getName()
                        }));
    register(
        "reload",
        "",
        "Reloads the plugin configuration and store connection.",
        ctx -> {
          platform.loadConfig();
          String key = platform.getConfig().getSecretKey();
          if (key == null || key.isEmpty())
            return lines("No secret key configured; the current connection is unchanged.");
          return platform.connect(key, false).thenApply(info -> new String[] {"Reload completed."});
        });
    register(
        "debug",
        "<true/false>",
        "Enables or disables debug logging.",
        ctx -> {
          String value = ctx.getArguments()[0];
          if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false"))
            return lines("Specify 'true' or 'false' for debug mode.");
          ServerPlatformConfig config = platform.getConfig();
          config.setVerbose(Boolean.parseBoolean(value));
          platform.saveConfig(config);
          return lines("Debug mode set to " + value);
        });
    register(
        "forcecheck", "", "Checks immediately for purchases.", ctx -> platform.checkCommandQueue());
    register(
        "info",
        "",
        "Shows information about the connected store.",
        ctx ->
            lines(
                platform.getStoreServer().getName()
                    + " for webstore "
                    + platform.getAccount().getName(),
                "Server prices are in " + platform.getAccount().getCurrency().getIso4217(),
                "Webstore URL: " + platform.getAccount().getDomain()));
    register(
        "checkout",
        "<packageId>",
        "Creates a payment link for a package.",
        ctx -> {
          if (ctx.isFromConsole()) return lines("This command cannot be run from the console.");
          int id = Integer.parseInt(ctx.getArguments()[0]);
          return platform
              .request((api, secret) -> api.createCheckoutUrl(secret, id, ctx.getSenderUsername()))
              .thenApply(
                  url -> {
                    platform.sendCheckoutLink(ctx.getSenderUsername(), url.getUrl());
                    return new String[] {"Checkout link sent to you."};
                  });
        });
    register(
        "sendlink",
        "<packageId> <username>",
        "Sends a purchase link to a player.",
        ctx -> {
          int id = Integer.parseInt(ctx.getArguments()[0]);
          String username = CommandArguments.join(ctx.getArguments(), 1);
          return platform
              .callPlayer(username, () -> platform.isPlayerOnline(username))
              .thenCompose(
                  online -> {
                    if (!online)
                      return lines(username + " must be online to receive a package link.");
                    return platform
                        .request((api, secret) -> api.createCheckoutUrl(secret, id, username))
                        .thenApply(
                            url -> {
                              platform.sendCheckoutLink(username, url.getUrl());
                              return new String[] {"Checkout link sent to " + username};
                            });
                  });
        });
    register(
        "ban",
        "<playerName> <reason> <ip>",
        "Bans a user from the webstore.",
        ctx -> {
          String[] args = ctx.getArguments();
          String name = CommandArguments.join(args, 0, Math.max(1, args.length - 2));
          String reason = args.length > 2 ? args[args.length - 2] : args.length > 1 ? args[1] : "";
          String ip = args.length > 2 ? args[args.length - 1] : "";
          return platform
              .request((api, secret) -> api.createBan(secret, name, ip, reason))
              .thenApply(
                  success ->
                      new String[] {
                        Boolean.TRUE.equals(success)
                            ? "User " + name + " has been banned successfully."
                            : "Failed to ban user."
                      });
        });
    register(
        "lookup",
        "<username>",
        "Gets user transaction info.",
        ctx ->
            platform
                .request(
                    (api, secret) ->
                        api.getPlayerLookupInfo(
                            secret, CommandArguments.join(ctx.getArguments(), 0)))
                .thenApply(
                    info -> {
                      if (info == null) return new String[] {"No customer found."};
                      return new String[] {
                        "Username: " + info.getPlayer().getUsername(),
                        "Id: " + info.getPlayer().getId(),
                        "Chargeback Rate: " + info.getChargebackRate() + "%",
                        "Bans Total: " + info.getBanCount(),
                        "Payments: " + info.getPayments().size()
                      };
                    }));
    register(
        "goals",
        "",
        "Shows progress to community goals.",
        ctx ->
            platform
                .request((api, secret) -> api.getCommunityGoals(secret))
                .thenApply(
                    goals -> {
                      List<String> out = new ArrayList<>();
                      goals.stream()
                          .filter(
                              goal ->
                                  goal.getStatus() != io.tebex.model.CommunityGoal.Status.DISABLED)
                          .forEach(
                              goal ->
                                  out.add(
                                      String.format(
                                          Locale.ROOT,
                                          "%s (%.2f/%.2f) [%s]",
                                          goal.getName(),
                                          goal.getCurrent(),
                                          goal.getTarget(),
                                          goal.getStatus())));
                      if (out.isEmpty()) out.add("No community goals available.");
                      return out.toArray(new String[0]);
                    }));
  }
}
