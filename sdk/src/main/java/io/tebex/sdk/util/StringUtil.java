package io.tebex.sdk.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;

public class StringUtil {
    private static final DateTimeFormatter LEGACY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter MODERN_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssxxx");

    private static final HashSet<String> TRUTHY_STRINGS = new HashSet<>(Arrays.asList("true", "yes", "on", "1", "enabled", "enable", "cap"));

    private static final HashSet<String> FALSY_STRINGS = new HashSet<>(Arrays.asList("false", "no", "off", "0", "disabled", "disable", "nocap"));

    public static String pluralise(int count, String singular, String plural) {
        return count == 1 ? singular : plural;
    }
    public static String pluralise(int count, String word) {
        return pluralise(count, word, word + "s");
    }

    public static ZonedDateTime toLegacyDate(String date) {
        return LocalDateTime.parse(date, LEGACY_FORMATTER).atZone(ZoneId.of("UTC"));
    }

    public static ZonedDateTime toModernDate(String date) {
        return LocalDateTime.parse(date, MODERN_FORMATTER).atZone(ZoneId.of("UTC"));
    }

    public static boolean isTruthy(String input)
    {
        if (input == null || input.isEmpty())
        {
            return false;
        }

        return TRUTHY_STRINGS.contains(input.trim().toLowerCase());
    }

    public static boolean isFalsy(String input)
    {
        if (input == null || input.isEmpty())
        {
            return true;
        }

        return FALSY_STRINGS.contains(input.trim().toLowerCase());
    }
}
