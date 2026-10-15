package io.tebex.minecraft.util;

import java.util.UUID;

public class UUIDUtil {
  public static final UUID EMPTY_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

  /**
   * Translates a Mojang-style UUID into an UUID Java can use. The Tebex plugin API returns all
   * results with Mojang-style UUIDs.
   *
   * @param id the Mojang UUID to use
   * @return the Java UUID or null if id provided is null
   */
  public static UUID mojangIdToJavaId(String id) {
    if (id == null || id.isEmpty() || id.equals("null")) {
      return EMPTY_UUID; // empty uuid
    }

    return UUID.fromString(
        id.replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5"));
  }
}
