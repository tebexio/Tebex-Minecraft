package io.tebex.minecraft.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.tebex.minecraft.platform.config.ServerPlatformConfig;
import java.io.File;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RuntimeConfigurationTest {
  @TempDir Path directory;

  @Test
  void loadsEnvironmentOverrideAndPersistsMutableValues() {
    Host host = new Host(directory.toFile());
    try {
      RuntimeConfiguration environment = new RuntimeConfiguration(host, () -> "environment-key");
      ServerPlatformConfig loaded = environment.load();
      assertEquals("environment-key", loaded.getSecretKey());
      assertEquals("buy", loaded.getBuyCommandName());
      assertTrue(loaded.isBuyCommandEnabled());

      loaded.setSecretKey("persisted-key");
      loaded.setVerbose(true);
      environment.save(loaded);

      ServerPlatformConfig reloaded = new RuntimeConfiguration(host, () -> null).load();
      assertEquals("persisted-key", reloaded.getSecretKey());
      assertTrue(reloaded.isVerbose());
    } finally {
      host.shutdown();
    }
  }

  private static final class Host extends MockPluginPlatform {
    private final File directory;

    Host(File directory) {
      this.directory = directory;
    }

    @Override
    public File getRunningDirectory() {
      return directory;
    }
  }
}
