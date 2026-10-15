package io.tebex.minecraft.commands;

public class Responder {
  public static String[] formatWelcome(Context context, String pluginVersion) {
    return new String[] {
      formatFancy(context, "Welcome to Tebex!"),
      formatFancy(context, "This server is running version {0}", "v" + pluginVersion)
    };
  }

  public static String formatFancy(Context context, String message, String... args) {
    // "Tebex" appears always in cyan from the raw message
    if (!context.isFromConsole()) {
      message = message.replaceAll("Tebex", "\u00A7bTebex");
    }

    // Insert args and color them gold
    for (int i = 0; i < args.length; i++) {
      String placeholder = "{" + i + "}";

      // underline addresses and append positional arg colored gold
      if (!context.isFromConsole()) {
        if (args[i].contains("https://")) {
          args[i] = args[i].replace(args[i], "\u00A7n" + args[i]);
        }

        message = message.replace(placeholder, "\u00A76" + args[i] + "\u00A7f");
      } else { // otherwise for the console just replace the positional arg without coloring
        message = message.replace(placeholder, args[i]);
      }
    }

    // Stop before adding a colored prefix if this is a console response
    if (context.isFromConsole()) {
      return message;
    }

    // Otherwise append a prefix (cyan), message is white
    return "\u00A7b[Tebex] \u00A7f" + message;
  }

  public static String formatError(Context context, String message) {
    if (context.isFromConsole()) {
      return message;
    }

    // "Tebex" appears always in cyan
    message = message.replaceAll("Tebex", "\u00A7bTebex");

    // Prefix is cyan, message is red
    return "\u00A7b[Tebex] \u00A7c" + message;
  }

  public static String formatSuccess(Context context, String message, String... args) {
    if (context.isFromConsole()) {
      return message;
    }

    // "Tebex" appears always in cyan
    message = message.replaceAll("Tebex", "\u00A7bTebex");

    // Insert args and color them gold
    for (int i = 0; i < args.length; i++) {
      String placeholder = "{" + i + "}";
      message = message.replace(placeholder, "\u00A76" + args[i] + "\u00A7f");
    }

    // Prefix is cyan, message is white
    return "\u00A7b[Tebex] \u00A7a" + message;
  }
}
