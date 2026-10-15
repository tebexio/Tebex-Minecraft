package io.tebex.minecraft.platform.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import lombok.Getter;
import lombok.Setter;

/**
 * Configuration values shared by the Minecraft runtime and native server adapters. This is the
 * "full" Tebex configuration.
 */
@Getter
@Setter
public class ServerPlatformConfig {
  private final int configVersion;
  private YamlDocument yamlDocument;

  private String buyCommandName;
  private boolean buyCommandEnabled;
  private boolean checkForUpdates;
  private boolean verbose;
  private boolean proxyMode;
  private String secretKey;

  private boolean autoReportEnabled;

  /**
   * Creates a PlatformConfig instance with the provided configuration version.
   *
   * @param configVersion The configuration version.
   */
  public ServerPlatformConfig(int configVersion) {
    this.configVersion = configVersion;
  }

  /**
   * Returns the configuration version.
   *
   * @return The configuration version.
   */
  public int getConfigVersion() {
    return configVersion;
  }

  /**
   * Returns the secret key.
   *
   * @return The secret key.
   */
  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String key) {
    this.secretKey = key;
  }

  public boolean isVerbose() {
    return verbose;
  }

  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  /**
   * Returns the YAML document for this configuration.
   *
   * @return The YAML document.
   */
  public YamlDocument getYamlDocument() {
    return yamlDocument;
  }
}
