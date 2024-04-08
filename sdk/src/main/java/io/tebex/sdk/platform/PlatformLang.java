package io.tebex.sdk.platform;

public enum PlatformLang {
    ERROR_OCCURRED("&cAn error occurred: {0}"),

    NOT_CONNECTED_TO_STORE("&cThis server is not connected to a webstore. Use &f/tebex secret <key> &cto set your store key."),
    NOT_CONNECTED_TO_ANALYTICS("&cThis server is not connected to analytics. Use &f/analytics secret <key> &cto set your analytics key."),

    INVALID_SECRET_KEY("&cServer not found. Please check your secret key."),
    SUCCESSFULLY_CONNECTED("Connected to &b{0}&7."),
    ALREADY_CONNECTED("This server is already connected to a webstore."),

    NO_PERMISSION("&cYou do not have permission to use this command."),
    INVALID_USAGE("&cInvalid command usage. Use /{0} {1}"),
    UNKNOWN_COMMAND("&cUnknown command."),

    FAILED_TO_CREATE_CHECKOUT_URL("&cFailed to create checkout URL. Please contact an administrator."),
    CHECKOUT_URL("&aA checkout link has been created for you. Click here to complete payment: &f{0}"),

    RELOAD_SUCCESS("&aSuccessfully reloaded."),
    RELOAD_FAILURE("&cFailed to reload the plugin! Check console for more information."),

    PLAYER_NOT_FOUND("&cPlayer not found."),
    MUST_BE_PLAYER("&cYou must be a player to run this command!"),
    EVENT_TRACKED("&aEvent tracked."),
    ;

    private final String message;

    PlatformLang(String message) {
        this.message = message;
    }

    public String get() {
        return message;
    }

    public String get(String... args) {
        String message = this.message;
        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", args[i]);
        }
        return message;
    }
}
