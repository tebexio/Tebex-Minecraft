package io.tebex.plugin;

public enum Lang {
    NOT_CONNECTED("&cThis server is not connected to a webstore. Use &f/tebex secret <key> &cto set your store key."),
    NO_PERMISSION("&cYou do not have permission to use this command."),
    INVALID_USAGE("&cInvalid command usage. Use /{0} {1}"),
    COMMAND_ERROR("&cAn error occurred: {0}"),
    FAILED_TO_CREATE_CHECKOUT_URL("&cFailed to create checkout URL. Please contact an administrator."),
    CHECKOUT_URL("&aA checkout link has been created for you. Click here to complete payment: &f{0}")
    ;

    private final String message;

    Lang(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String getMessage(String... args) {
        String message = this.message;
        for (int i = 0; i < args.length; i++) {
            message = message.replace("{" + i + "}", args[i]);
        }
        return message;
    }
}
