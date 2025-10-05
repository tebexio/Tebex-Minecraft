package io.tebex.sdk.util;

import java.util.UUID;

public class UUIDUtil {

    /**
     * Translates a Mojang-style UUID into a Java UUID. Returns null for null/empty/"null" or invalid input.
     *
     * @param id the Mojang UUID to use
     * @return the Java UUID or null if id provided is null
     */
    public static UUID mojangIdToJavaId(String id) {
        if (id == null || id.isEmpty() || "null".equalsIgnoreCase(id)) {
            return null;
        }

        try {
            String uuid = id.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
            return UUID.fromString(uuid);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
