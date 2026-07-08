package io.tebex.sdk.commands;

import java.util.Arrays;

public final class CommandArguments {
    private CommandArguments() {}

    public static String join(String[] arguments, int startIndex) {
        return join(arguments, startIndex, arguments.length);
    }

    public static String join(String[] arguments, int startIndex, int endIndex) {
        if (arguments == null || startIndex >= arguments.length || startIndex >= endIndex) {
            return "";
        }

        int safeStartIndex = Math.max(0, startIndex);
        int safeEndIndex = Math.min(arguments.length, endIndex);
        return stripWrappingQuotes(String.join(" ", Arrays.copyOfRange(arguments, safeStartIndex, safeEndIndex)));
    }

    private static String stripWrappingQuotes(String argument) {
        if (argument.length() >= 2 && argument.startsWith("\"") && argument.endsWith("\"")) {
            return argument.substring(1, argument.length() - 1);
        }

        return argument;
    }
}
