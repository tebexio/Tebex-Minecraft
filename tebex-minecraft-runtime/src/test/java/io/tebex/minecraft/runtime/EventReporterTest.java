package io.tebex.minecraft.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.tebex.model.PluginEvent;
import org.junit.jupiter.api.Test;

class EventReporterTest {
  @Test
  void boundsSnapshotsAndClearsBothEventKinds() {
    MockPluginPlatform host = new MockPluginPlatform();
    EventReporter reporter = new EventReporter(host.getRuntime());
    try {
      for (int index = 0; index < 1100; index++) {
        reporter.recordJoin("uuid-" + index, "player-" + index, "127.0.0.1");
        reporter.recordPlugin(new PluginEvent(PluginEvent.Level.INFO, "event-" + index));
      }

      assertEquals(750, reporter.joinSnapshot().size());
      assertEquals(750, reporter.pluginSnapshot().size());
      reporter.clear();
      assertTrue(reporter.joinSnapshot().isEmpty());
      assertTrue(reporter.pluginSnapshot().isEmpty());
    } finally {
      host.shutdown();
    }
  }
}
