package io.tebex.minecraft.commands;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Native permission-node registration defaults, preserving Forge/NeoForge's historical info policy.
 */
public final class PermissionDefaults {
  private PermissionDefaults() {}

  public static Map<String, Boolean> nodes(boolean publicInfo) {
    Map<String, Boolean> defaults = new LinkedHashMap<>();
    for (String node : new String[] {"base", "buy", "help", "checkout", "goals"})
      defaults.put(node, true);
    for (String node :
        new String[] {
          "admin", "ban", "debug", "forcecheck", "lookup", "reload", "secret", "sendlink"
        }) defaults.put(node, false);
    defaults.put("info", publicInfo);
    return Collections.unmodifiableMap(defaults);
  }
}
