package io.tebex.sdk.analytics.response;

public class PluginInformation {
    private final String versionName;
    private final Integer versionNumber;
    private final String downloadUrl;

    /**
     * Constructs a PluginInformation instance.
     *
     * @param versionName The version name of the plugin.
     * @param versionNumber The version number of the plugin.
     * @param downloadUrl The download URL for the plugin.
     */
    public PluginInformation(String versionName, Integer versionNumber, String downloadUrl) {
        this.versionName = versionName;
        this.versionNumber = versionNumber;
        this.downloadUrl = downloadUrl;
    }

    /**
     * Returns the version name of the plugin.
     *
     * @return The version name.
     */
    public String getVersionName() {
        return versionName;
    }

    /**
     * Returns the version number of the plugin.
     *
     * @return The version number.
     */
    public Integer getVersionNumber() {
        return versionNumber;
    }

    /**
     * Returns the download URL for the plugin.
     *
     * @return The download URL.
     */
    public String getDownloadUrl() {
        return downloadUrl;
    }

    @Override
    public String toString() {
        return "PluginInformation{" +
                "versionName='" + versionName + '\'' +
                ", versionNumber=" + versionNumber +
                ", downloadUrl='" + downloadUrl + '\'' +
                '}';
    }
}