package io.tebex.sdk.platform;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The PlatformTelemetry class contains information about the server platform
 * and environment, such as server software, plugin version, and Java version.
 */
public class PlatformTelemetry {
    private final String pluginVersion;
    private final String serverSoftware;
    private final String serverVersion;
    private final String javaVersion;
    private final String systemArch;
    private final boolean onlineMode;

    /**
     * Creates a PlatformTelemetry instance with the provided information.
     *
     * @param pluginVersion The plugin version.
     * @param serverSoftware The server software name.
     * @param serverVersion The server version.
     * @param onlineMode The server's online mode status.
     */
    public PlatformTelemetry(String pluginVersion, String serverSoftware, String serverVersion, boolean onlineMode) {
        Pattern pattern = Pattern.compile("MC: (\\d+\\.\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(serverVersion);
        if (matcher.find()) {
            serverVersion = matcher.group(1);
        }

        this.pluginVersion = pluginVersion;
        this.serverSoftware = serverSoftware;
        this.serverVersion = serverVersion;
        this.javaVersion = System.getProperty("java.version");
        this.systemArch = System.getProperty("os.arch");
        this.onlineMode = onlineMode;
    }

    /**
     * Retrieves the plugin version.
     *
     * @return The plugin version.
     */
    public String getPluginVersion() {
        return pluginVersion;
    }

    /**
     * Retrieves the server software name.
     *
     * @return The server software name.
     */
    public String getServerSoftware() {
        return serverSoftware;
    }

    /**
     * Retrieves the server version.
     *
     * @return The server version.
     */
    public String getServerVersion() {
        return serverVersion;
    }

    /**
     * Retrieves the Java version.
     *
     * @return The Java version.
     */
    public String getJavaVersion() {
        return javaVersion;
    }

    /**
     * Retrieves the system architecture.
     *
     * @return The system architecture.
     */
    public String getSystemArch() {
        return systemArch;
    }

    /**
     * Checks if the server is running in online mode.
     *
     * @return True if the server is in online mode, false otherwise.
     */
    public boolean isOnlineMode() {
        return onlineMode;
    }

    /**
     * Generates a string representation of the PlatformTelemetry object.
     *
     * @return A string representation of the PlatformTelemetry object.
     */
    @Override
    public String toString() {
        return "PlatformTelemetry{" +
                "pluginVersion='" + pluginVersion + '\'' +
                ", serverSoftware='" + serverSoftware + '\'' +
                ", serverVersion='" + serverVersion + '\'' +
                ", javaVersion='" + javaVersion + '\'' +
                ", systemArch='" + systemArch + '\'' +
                ", onlineMode=" + onlineMode +
                '}';
    }
}