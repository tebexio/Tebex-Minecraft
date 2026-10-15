package io.tebex.minecraft.runtime;

import static io.tebex.minecraft.util.ResourceUtil.getBundledFile;

import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.minecraft.platform.PlatformHost;
import io.tebex.minecraft.platform.config.ServerPlatformConfig;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;

/** Owns YAML loading, environment overrides, persistence, and legacy config migration. */
final class RuntimeConfiguration {
  private final PlatformHost host;
  private final Supplier<String> environmentSecret;

  RuntimeConfiguration(PlatformHost host, Supplier<String> environmentSecret) {
    this.host = Objects.requireNonNull(host, "host");
    this.environmentSecret = Objects.requireNonNull(environmentSecret, "environmentSecret");
  }

  ServerPlatformConfig load() {
    try {
      ServerPlatformConfig config = parse(open());
      String override = environmentSecret.get();
      if (override != null && !override.isEmpty()) config.setSecretKey(override);
      return config;
    } catch (IOException error) {
      throw new IllegalStateException("Cannot load Tebex configuration", error);
    }
  }

  YamlDocument open() throws IOException {
    try (InputStream input = Files.newInputStream(configFile().toPath())) {
      return YamlDocument.create(input);
    }
  }

  ServerPlatformConfig parse(YamlDocument yaml) {
    ServerPlatformConfig value = new ServerPlatformConfig(yaml.getInt("config-version", 2));
    value.setYamlDocument(yaml);
    value.setSecretKey(yaml.getString("server.secret-key", ""));
    value.setBuyCommandName(yaml.getString("buy-command.name", "buy"));
    value.setBuyCommandEnabled(yaml.getBoolean("buy-command.enabled", true));
    value.setCheckForUpdates(yaml.getBoolean("check-for-updates", true));
    value.setVerbose(yaml.getBoolean("verbose", false));
    value.setProxyMode(yaml.getBoolean("server.proxy", false));
    value.setAutoReportEnabled(yaml.getBoolean("auto-report-enabled", true));
    return value;
  }

  void save(ServerPlatformConfig config) {
    try {
      YamlDocument yaml = Objects.requireNonNull(config.getYamlDocument(), "config YAML");
      yaml.set("server.secret-key", config.getSecretKey());
      yaml.set("verbose", config.isVerbose());
      yaml.save(configFile());
    } catch (IOException error) {
      throw new IllegalStateException("Cannot save Tebex configuration", error);
    }
  }

  void migrateLegacy(Properties properties) {
    try {
      YamlDocument yaml = open();
      yaml.set("buy-command.name", properties.getProperty("buy-command-name", "buy"));
      yaml.set(
          "buy-command.enabled",
          !Boolean.parseBoolean(properties.getProperty("disable-buy-command", "false")));
      yaml.set(
          "check-for-updates",
          Boolean.parseBoolean(properties.getProperty("check-for-updates", "true")));
      yaml.set("verbose", Boolean.parseBoolean(properties.getProperty("verbose", "false")));
      yaml.set(
          "server.proxy", Boolean.parseBoolean(properties.getProperty("is-bungeecord", "false")));
      yaml.set("server.secret-key", properties.getProperty("server-key", ""));
      yaml.save(configFile());
    } catch (IOException error) {
      throw new IllegalStateException("Cannot migrate legacy Tebex configuration", error);
    }
  }

  private File configFile() {
    return getBundledFile(host, host.getRunningDirectory(), "config.yml");
  }
}
