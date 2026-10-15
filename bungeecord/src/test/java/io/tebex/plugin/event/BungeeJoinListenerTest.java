package io.tebex.plugin.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.tebex.minecraft.runtime.TebexRuntime;
import io.tebex.plugin.BungeePlatform;
import java.net.InetSocketAddress;
import java.util.UUID;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import org.junit.jupiter.api.Test;

class BungeeJoinListenerTest {
  @Test
  void forwardsLoginIdentityAndAddressToRuntime() {
    BungeePlatform platform = mock(BungeePlatform.class);
    TebexRuntime runtime = mock(TebexRuntime.class);
    when(platform.runtime()).thenReturn(runtime);
    PendingConnection connection = mock(PendingConnection.class);
    UUID id = UUID.randomUUID();
    when(connection.getUniqueId()).thenReturn(id);
    when(connection.getName()).thenReturn("JustCain");
    when(connection.getAddress()).thenReturn(new InetSocketAddress("192.0.2.10", 25565));
    LoginEvent event = mock(LoginEvent.class);
    when(event.getConnection()).thenReturn(connection);

    new JoinListener(platform).onPlayerConnect(event);

    verify(runtime).onPlayerJoin("JustCain", id, "192.0.2.10");
  }
}
