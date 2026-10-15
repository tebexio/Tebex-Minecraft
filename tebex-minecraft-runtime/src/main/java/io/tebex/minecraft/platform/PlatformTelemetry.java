package io.tebex.minecraft.platform;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

/**
 * The PlatformTelemetry class contains information about the server platform and environment, such
 * as server software, plugin version, and Java version.
 */
@Getter
@AllArgsConstructor
@ToString
public class PlatformTelemetry {
  private static final java.util.regex.Pattern MINECRAFT_VERSION =
      java.util.regex.Pattern.compile("MC: ([0-9]+(?:\\.[0-9]+)+)");

  public static PlatformTelemetry current(
      String pluginVersion, String software, String serverVersion, boolean onlineMode) {
    java.util.regex.Matcher match = MINECRAFT_VERSION.matcher(serverVersion);
    return new PlatformTelemetry(
        pluginVersion,
        software,
        match.find() ? match.group(1) : serverVersion,
        System.getProperty("java.version"),
        System.getProperty("os.arch"),
        onlineMode);
  }

  private final String pluginVersion;
  private final String serverSoftware;
  private final String serverVersion;
  private final String javaVersion;
  private final String systemArch;
  private final boolean onlineMode;
}
