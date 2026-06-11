package io.tebex.sdk.commands;

import io.tebex.sdk.obj.CommunityGoal;
import io.tebex.sdk.platform.PluginPlatform;
import io.tebex.sdk.platform.config.IPlatformConfig;
import io.tebex.sdk.request.response.ServerInformation;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Contains all standard implementations of basic Tebex commands.
 */
public class TebexCommands {
    public static final String TEBEX_COMMAND_PREFIX = "tebex";

    @Getter private static PluginPlatform platform;

    /**
     * A map of the command name to the command instance, containing its handler
     */
    private static final Map<String, PlayerCommand> commands = new HashMap<>();

    /**
     * Each platform can define a restricted set of commands, which only those commands will be executed.
     * If this is empty, all default Tebex commands will be enabled.
     */
    private static final ArrayList<String> restrictedToCommands = new ArrayList<>();

    private TebexCommands() {}

    private static void register(String name, String usage, String description, Function<Context, CompletableFuture<String[]>> handler) {
        PlayerCommand command = new PlayerCommand(name, usage, description, handler);
        commands.put(name, command);
    }

    private static String joinArguments(String[] arguments, int startIndex) {
        return String.join(" ", Arrays.copyOfRange(arguments, startIndex, arguments.length));
    }

    public static Map<String, PlayerCommand> getCommands() {
        return Collections.unmodifiableMap(commands);
    }

    public static void setRestrictedToCommands(String... commands) {
        restrictedToCommands.clear();
        restrictedToCommands.addAll(Arrays.asList(commands));
    }

    /**
     * Processes an in-progress command given a context.
     *
     * @param context The context in which the command is being executed (who, what, etc.)
     * @param sendMessageConsumer Some function to consume the responses from this command
     * @return True if the command was recognized and processable.
     */
    public static boolean process(Context context, Consumer<CompletableFuture<String[]>> sendMessageConsumer) {
        if (!context.getFullCommand().startsWith(TEBEX_COMMAND_PREFIX)) {
            return false;
        }

        // Find command in set of registered commands
        CompletableFuture<String[]> response = new CompletableFuture<>();
        PlayerCommand command = commands.get(context.getCommandName().replaceAll(TEBEX_COMMAND_PREFIX + " ", ""));
        if (command == null) {
            response.complete(new String[]{Responder.formatError(context, "Command not found.")});
            sendMessageConsumer.accept(response);
            return false;
        }

        // Ensure this command isn't restricted on this platform (bungee and velocity don't need certain commands implemented)
        if (!restrictedToCommands.isEmpty() && !restrictedToCommands.contains(command.getCommandName())) {
            response.complete(new String[]{Responder.formatError(context, "Command is restricted on this platform.")});
            sendMessageConsumer.accept(response);
            return false;
        }

        // Check permissions against the platform for the sending user if we're a player
        if (!context.isFromConsole()) {
            boolean hasPermission = TebexCommands.getPlatform().hasPermission(context.getSenderUsername(), command.getPermission());
            if (!hasPermission) {
                response.complete(new String[]{Responder.formatError(context, "Unrecognized command or no permission.")});
                sendMessageConsumer.accept(response);
                return false;
            }
        }

        // Check number of args required vs args provided in the context
        if (context.getArguments().length < command.getNumArgsRequired()) {
            response.complete(new String[]{Responder.formatError(context, "Usage: /tebex " + command.getCommandName() + " " + command.getUsage())});
            sendMessageConsumer.accept(response);
            return false;
        }

        // Command is found and params are valid to run, get its handler func and run it with our context
        Function<Context, CompletableFuture<String[]>> handler = command.getHandler();
        sendMessageConsumer.accept(handler.apply(context));
        return true;
    }

    /**
     * Returns the commands that the player has permission for.
     *
     * @param username The player's username.
     * @return A map of available command names to the command references.
     */
    public static Map<String, PlayerCommand> getAllowedCommands(String username) {
        Map<String, PlayerCommand> allowedCommands = new HashMap<>();
        for (Map.Entry<String, PlayerCommand> entry : commands.entrySet()) {
            PlayerCommand command = entry.getValue();

            // Only show commands the user has permissions for
            if (!platform.hasPermission(username, command.getPermission())) {
                continue;
            }

            allowedCommands.put(entry.getKey(), command);
        }
        return allowedCommands;
    }

    /**
     * Registers all Tebex commands on the given platform.
     * @param platform The platform we are registering on.
     */
    public static void init(PluginPlatform platform) {
        TebexCommands.platform = platform;

        register("ban","<playerName> <reason> <ip>", "Bans a user from the webstore.", (ctx) -> {
            String[] arguments = ctx.getArguments();
            String name = String.join(" ", Arrays.copyOfRange(arguments, 0, arguments.length - 2)); // player may be offline, so don't rely on target username which requires successful getPlayer() call
            String reason = arguments[arguments.length - 2];
            String ip = arguments[arguments.length - 1];

            CompletableFuture<String[]> response = new CompletableFuture<>();
            platform.getSDK().createBan(name, ip, reason).thenAccept((success) -> {
                if (success) {
                    response.complete(new String[] {Responder.formatSuccess(ctx, "User {0} has been banned successfully.", name)});
                } else {
                    response.complete(new String[] {Responder.formatError(ctx, "Failed to ban user.")});
                }
            }).exceptionally(e -> {
                response.complete(new String[] {Responder.formatError(ctx, "Failed to ban user. " + e.getMessage())});
                return null;
            });
            return response;
        });


        register("checkout", "<packageId>", "Creates a payment link for a package.", (ctx) -> {
            CompletableFuture<String[]> response = new CompletableFuture<>();
            if (ctx.isFromConsole()) {
                response.complete(new String[] {Responder.formatError(ctx, "This command cannot be run from the console.")});
                return response;
            }

            String packageId = ctx.getArguments()[0];
            int intPackageId = -1;
            try {
                intPackageId = Integer.parseInt(packageId);
            } catch (NumberFormatException e) {
                response.complete(new String[] {Responder.formatError(ctx, "The Package ID must be a number.")});
                return response;
            }

            platform.getSDK().createCheckoutUrl(intPackageId, ctx.getSenderUsername()).thenAccept((url) -> {
                platform.sendCheckoutLink(ctx.getSenderUsername(), url.getUrl());
                response.complete(new String[] { Responder.formatSuccess(ctx, "Checkout link sent to you.")});
            }).exceptionally(e -> {
                response.complete(new String[] { Responder.formatError(ctx, e.getMessage())});
                return null;
            });

            return response;
        });


        register("debug", "<true/false>", "Enables or disables debug logging.", (ctx) -> {
            CompletableFuture<String[]> response = new CompletableFuture<>();
            String value = ctx.getArguments()[0].toLowerCase();
            if (!value.equals("true") && !value.equals("false")) {
                response.complete(new String[] { Responder.formatError(ctx, "Specify 'true' or 'false' for debug mode.")});
                return response;
            }

            boolean debugValue = Boolean.parseBoolean(value);
            IPlatformConfig cfg = platform.getPlatformConfig();
            cfg.setVerbose(debugValue);

            // save debug config to file
            platform.saveConfig(cfg);

            response.complete(new String[] { Responder.formatFancy(ctx, "Debug mode set to {0}", value) } );
            return response;
        });


        register("forcecheck", "", "Checks immediately for any purchases that need to be delivered.", (ctx) -> {
            boolean previousDebugState = platform.getPlatformConfig().isVerbose();
            // Temporarily enable debug logging while a forcecheck runs
            platform.getPlatformConfig().setVerbose(true);

            CompletableFuture<String[]> response = platform.checkCommandQueue(false);

            // Restore previous debug state
            platform.getPlatformConfig().setVerbose(previousDebugState);
            response.complete(new String[]{Responder.formatSuccess(ctx, "Check completed. See console for details.")});
            return response;
        });


        register("goals", "", "Shows progress to community goals.", (ctx) -> {
            CompletableFuture<String[]> response = new CompletableFuture<>();
            platform.getSDK().getCommunityGoals().thenAccept((goals) -> {
                if (goals.isEmpty()) {
                    response.complete(new String[]{Responder.formatFancy(ctx, "No community goals available.")});
                    return;
                }

                ArrayList<String> goalsResponse = new ArrayList<>();
                goalsResponse.add(Responder.formatFancy(ctx, "Community Goals: "));
                for (CommunityGoal goal: goals) {
                    if (goal.getStatus() != CommunityGoal.Status.DISABLED) {
                        goalsResponse.add(Responder.formatFancy(ctx, String.format("- %s (%.2f/%.2f) [%s]", goal.getName(), goal.getCurrent(), goal.getTarget(), goal.getStatus())));
                    }
                }
                response.complete(goalsResponse.toArray(new String[0]));
            });

            return response;
        });

        register("info", "", "Shows information about the connected store.", (ctx) -> {
            CompletableFuture<String[]> response = new CompletableFuture<>();

            ServerInformation.Store store = platform.getStore();
            ServerInformation.Server server = platform.getStoreServer();

            ArrayList<String> infoResponse = new ArrayList<>();
            infoResponse.add(Responder.formatFancy(ctx, "Information for this server:"));
            infoResponse.add(Responder.formatFancy(ctx, "{0} for webstore {1}", server.getName(), store.getName()));
            infoResponse.add(Responder.formatFancy(ctx, "Server prices are in {0}", store.getCurrency().getIso4217()));
            infoResponse.add(Responder.formatFancy(ctx, "Webstore URL: {0}", store.getDomain()));
            response.complete(infoResponse.toArray(new String[0]));
            return response;
        });

        register("lookup", "<username>", "Gets user transaction info from your webstore.", (ctx) -> {
            CompletableFuture<String[]> response = new CompletableFuture<>();

            String username = joinArguments(ctx.getArguments(), 0); // Use provided username as player is not required to be online / no instance required
            platform.getSDK().getPlayerLookupInfo(username).thenAccept((lookupInfo) -> {
                ArrayList<String> lookupResponse = new ArrayList<>();
                lookupResponse.add(Responder.formatFancy(ctx, "Username: {0}", lookupInfo.getPlayer().getUsername()));
                lookupResponse.add(Responder.formatFancy(ctx, "Id: {0}", lookupInfo.getPlayer().getId()));
                lookupResponse.add(Responder.formatFancy(ctx, "Chargeback Rate: {0}%", String.valueOf(lookupInfo.chargebackRate)));
                lookupResponse.add(Responder.formatFancy(ctx, "Bans Total: {0}", String.valueOf(lookupInfo.banCount)));
                lookupResponse.add(Responder.formatFancy(ctx, "Payments: {0}", String.valueOf(lookupInfo.payments.size())));
                response.complete(lookupResponse.toArray(new String[0]));
            }).exceptionally(e -> {
                response.complete(new String[] { Responder.formatError(ctx, e.getMessage())});
                return null;
            });

            return response;
        });

        register("reload", "", "Reloads the plugin configuration and store connection.", (ctx) -> {
            platform.reloadConfig();
            platform.refreshListings();
            platform.getSDK().sendPluginEvents();

            CompletableFuture<String[]> response = new CompletableFuture<>();
            response.complete(new String[]{Responder.formatSuccess(ctx, "Reload completed.")});
            return response;
        });

        register("secret", "<key>", "Connects to your store.", (ctx) -> {
            CompletableFuture<String[]> response = new CompletableFuture<>();

            platform.sendPlayerMessage(ctx.getSenderUsername(), Responder.formatFancy(ctx, "Checking your secret key..."));
            String oldKey = platform.getPlatformConfig().getSecretKey();

            // Set the new key and attempt to query server info. If it works, key is valid and we set in config.
            String newKey = ctx.getArguments()[0];

            IPlatformConfig cfg = platform.getPlatformConfig();
            platform.getSDK().setSecretKey(newKey);
            cfg.setSecretKey(newKey);
            platform.saveConfig(cfg);

            platform.getSDK().getServerInformation().thenAccept((newInfo) -> {
                if (newInfo != null) {
                    platform.setServerInformation(newInfo);
                    platform.refreshListings();
                    platform.initStore();
                    response.complete(new String[]{Responder.formatFancy(ctx, "Successfully connected to your store: {0} as {1}", newInfo.getStore().getName(), newInfo.getServer().getName())});
                }
            }).exceptionally((e) -> {
                platform.getSDK().setSecretKey(oldKey);
                cfg.setSecretKey(oldKey);
                platform.saveConfig(cfg);
                response.complete(new String[]{Responder.formatError(ctx, "Your secret key was invalid. Please try again.")});
                return null;
            });

            return response;
        });

        register("sendlink", "<packageId> <username>", "Sends a purchase link to a player.", (ctx) -> {
            CompletableFuture<String[]> response = new CompletableFuture<>();
            String packageId = ctx.getArguments()[0];
            String username = joinArguments(ctx.getArguments(), 1);
            int intPackageId = -1;

            try {
                intPackageId = Integer.parseInt(packageId);
            } catch (NumberFormatException e) {
                response.complete(new String[]{Responder.formatError(ctx, "Package ID must be a number.")});
                return response;
            }

            if (platform.getPlayer(username) == null) {
                response.complete(new String[]{Responder.formatError(ctx, username + " must be online to receive a package link.")});
                return response;
            }

            platform.getSDK().createCheckoutUrl(intPackageId, username).thenAccept(checkoutUrl -> {
                platform.sendCheckoutLink(username, checkoutUrl.getUrl());
                response.complete(new String[]{Responder.formatSuccess(ctx, "Checkout link sent to " + username)});
            }).exceptionally(e -> {
                response.complete(new String[]{Responder.formatError(ctx, "Failed to send checkout link: " + e.getMessage())});
                return null;
            });

            return response;
        });

        register("help", "", "Shows available commands.", (ctx) -> {
            CompletableFuture<String[]> response = new CompletableFuture<>();
            ArrayList<String> helpResponse = new ArrayList<>();

            helpResponse.add(Responder.formatFancy(ctx, "Available commands:"));
            for (Map.Entry<String, PlayerCommand> entry : commands.entrySet()) {
                PlayerCommand command = entry.getValue();

                // Check if the command is restricted for the platform, and skip if so
                if (!restrictedToCommands.isEmpty() && restrictedToCommands.contains(command.getCommandName())) {
                    continue;
                }

                // Only show commands the user has permissions for
                if (!ctx.isFromConsole()) {
                    boolean hasPermission = TebexCommands.getPlatform().hasPermission(ctx.getSenderUsername(), command.getPermission());
                    if (!hasPermission) {
                        continue;
                    }
                }

                StringBuilder helpMessage = new StringBuilder();
                helpMessage.append("/");
                helpMessage.append(TEBEX_COMMAND_PREFIX);
                helpMessage.append(" ");
                helpMessage.append(command.getCommandName());
                if (!command.getUsage().isEmpty()) {
                    helpMessage.append(" ");
                    helpMessage.append(command.getUsage());
                }
                helpMessage.append(" | ");
                helpMessage.append(command.getDescription());
                helpResponse.add(Responder.formatFancy(ctx, helpMessage.toString()));
            }
            response.complete(helpResponse.toArray(new String[0]));
            return response;
        });
    }
}
