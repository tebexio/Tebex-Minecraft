package io.tebex.sdk.platform.config;

import dev.dejvokep.boostedyaml.YamlDocument;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * The PlatformConfig class holds the configuration for the Tebex Plugin.
 * It contains settings related to excluded players, minimum playtime, and various other options.
 */
public class ServerPlatformConfig implements IPlatformConfig {
    private final int configVersion;
    private YamlDocument yamlDocument;

    /* Tebex Store specific settings */
    private String buyCommandName;
    private boolean buyCommandEnabled;
    private boolean checkForUpdates;
    private boolean verbose;
    private boolean proxyMode;
    private String storeSecretKey;

    private boolean autoReportEnabled;

    /* Tebex Analytics specific settings */
    private List<UUID> excludedPlayers;
    private boolean floodgateHook;
    private String analyticsSecretKey;
    private String bedrockPrefix;
    private boolean useServerFirstJoinedAt;
    private boolean developerMode;
    private boolean multiInstance;

    /**
     * Creates a PlatformConfig instance with the provided configuration version.
     *
     * @param configVersion The configuration version.
     */
    public ServerPlatformConfig(int configVersion) {
        this.configVersion = configVersion;
    }

    /**
     * Sets the secret key.
     *
     * @param secretKey The secret key.
     */
    public void setStoreSecretKey(String secretKey) {
        this.storeSecretKey = secretKey;
    }

    public void setBuyCommandName(String buyCommandName) {
        this.buyCommandName = buyCommandName;
    }

    public void setBuyCommandEnabled(boolean buyCommandEnabled) {
        this.buyCommandEnabled = buyCommandEnabled;
    }

    public void setCheckForUpdates(boolean checkForUpdates) {
        this.checkForUpdates = checkForUpdates;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setProxyMode(boolean proxyMode) {
        this.proxyMode = proxyMode;
    }

    public void setAutoReportEnabled(boolean autoReportEnabled) { this.autoReportEnabled = autoReportEnabled; }


    /**
     * Sets the list of excluded players.
     *
     * @param excludedPlayers The list of excluded player UUIDs.
     */
    public void setExcludedPlayers(List<UUID> excludedPlayers) {
        this.excludedPlayers = excludedPlayers != null ? excludedPlayers : Collections.emptyList();
    }

    /**
     * Sets whether to use the server's first joined timestamp for players.
     *
     * @param useServerFirstJoinedAt Whether to use the server's first joined timestamp.
     */
    public void setUseServerFirstJoinedAt(boolean useServerFirstJoinedAt) {
        this.useServerFirstJoinedAt = useServerFirstJoinedAt;
    }

    /**
     * Sets whether to use the developer mode.
     *
     * @param developerMode Whether to use the developer mode.
     */
    public void setDeveloperMode(boolean developerMode) {
        this.developerMode = developerMode;
    }
    /**
     * Sets whether to use the multi-instance mode.
     *
     * @param developerMode Whether to use multi-instance.
     */
    public void setMultiInstance(boolean multiInstance) {
        this.multiInstance = multiInstance;
    }

    /**
     * Sets the optional Bedrock Floodgate API hook
     *
     * @param floodgateHook Whether we should use the Floodgate API to detect Bedrock players instead.
     */
    public void setFloodgateHook(boolean floodgateHook) {
        this.floodgateHook = floodgateHook;
    }

    /**
     * Sets the server token.
     *
     * @param secretKey The server token.
     */
    public void setAnalyticsSecretKey(String secretKey) {
        this.analyticsSecretKey = secretKey;
    }

    /**
     * Sets the prefix for Bedrock Edition players.
     *
     * @param bedrockPrefix The prefix for Bedrock Edition players.
     */
    public void setBedrockPrefix(String bedrockPrefix) {
        this.bedrockPrefix = bedrockPrefix;
    }

    /**
     * Sets the YAML document for this configuration.
     *
     * @param yamlDocument The YAML document.
     */
    public void setYamlDocument(YamlDocument yamlDocument) {
        this.yamlDocument = yamlDocument;
    }

    /**
     * Returns the configuration version.
     *
     * @return The configuration version.
     */
    @Override
    public int getConfigVersion() {
        return configVersion;
    }

    /**
     * Returns the secret key.
     *
     * @return The secret key.
     */
    @Override
    public String getStoreSecretKey() {
        return storeSecretKey;
    }

    public String getBuyCommandName() {
        return buyCommandName;
    }

    public boolean isBuyCommandEnabled() {
        return buyCommandEnabled;
    }

    public boolean isCheckForUpdates() {
        return checkForUpdates;
    }

    @Override
    public boolean isVerbose() {
        return verbose;
    }

    public boolean isProxyMode() {
        return proxyMode;
    }

    public boolean isAutoReportEnabled() { return autoReportEnabled; }

    /* -------- Tebex Analytics -------- */

    /**
     * Returns the list of excluded players.
     *
     * @return The list of excluded player UUIDs.
     */
    public List<UUID> getExcludedPlayers() {
        return excludedPlayers;
    }

    /**
     * Checks if a player is excluded based on their UUID.
     *
     * @param uuid The player's UUID.
     * @return True if the player is excluded, false otherwise.
     */
    public boolean isPlayerExcluded(UUID uuid) {
        return excludedPlayers.contains(uuid);
    }

    /**
     * Checks if the server should use its first joined timestamp for players.
     *
     * @return True if the server should use its first joined timestamp, false otherwise.
     */
    public boolean shouldUseServerFirstJoinedAt() {
        return useServerFirstJoinedAt;
    }

    /**
     * Returns whether the plugin should run in developer mode (Local testing).
     *
     * @return True if the plugin should run in developer mode, false otherwise.
     */
    public boolean isDeveloperMode() {
        return developerMode;
    }

    /**
     * Returns whether the plugin should run in multi-instance mode (Used for Enterprise Networks).
     *
     * @return True if the plugin should be multi-instance mode.
     */
    public boolean isMultiInstance() {
        return multiInstance;
    }

    /**
     * Returns the server token.
     *
     * @return The server token.
     */
    public String getAnalyticsSecretKey() {
        return analyticsSecretKey;
    }

    /**
     * Returns the prefix for Bedrock Edition players.
     *
     * @return The prefix for Bedrock Edition players.
     */
    public String getBedrockPrefix() {
        return bedrockPrefix;
    }

    /**
     * Returns whether Analytics should hook into Floodgate.
     *
     * @return The {@link Boolean} that represents whether Analytics should hook into Floodgate's API or not.
     */
    public boolean isFloodgateHookEnabled() {
        return floodgateHook;
    }

    /**
     * Returns the YAML document for this configuration.
     *
     * @return The YAML document.
     */
    @Override
    public YamlDocument getYamlDocument() {
        return yamlDocument;
    }

    @Override
    public String toString() {
        return "ServerPlatformConfig{" +
                "configVersion=" + configVersion +
                ", yamlDocument=" + yamlDocument +
                ", buyCommandName='" + buyCommandName + '\'' +
                ", buyCommandEnabled=" + buyCommandEnabled +
                ", checkForUpdates=" + checkForUpdates +
                ", verbose=" + verbose +
                ", proxyMode=" + proxyMode +
                ", secretKey='" + storeSecretKey + '\'' +
                ", autoReportEnabled=" + autoReportEnabled +
                ", excludedPlayers=" + excludedPlayers +
                ", floodgateHook=" + floodgateHook +
                ", analyticsSecretKey='" + analyticsSecretKey + '\'' +
                ", bedrockPrefix='" + bedrockPrefix + '\'' +
                ", useServerFirstJoinedAt=" + useServerFirstJoinedAt +
                '}';
    }
}
