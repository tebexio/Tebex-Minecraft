package io.tebex.plugin.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.proxy.Player;
import io.tebex.minecraft.runtime.TebexRuntime;
import io.tebex.plugin.VelocityPlatform;
import java.net.InetSocketAddress;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VelocityJoinListenerTest {
  @Test
  void forwardsLoginIdentityAndAddressToRuntime() {
    VelocityPlatform platform = mock(VelocityPlatform.class);
    TebexRuntime runtime = mock(TebexRuntime.class);
    when(platform.runtime()).thenReturn(runtime);
    Player player = mock(Player.class);
    UUID id = UUID.randomUUID();
    when(player.getUsername()).thenReturn("JustCain");
    when(player.getUniqueId()).thenReturn(id);
    when(player.getRemoteAddress()).thenReturn(new InetSocketAddress("192.0.2.11", 25565));
    LoginEvent event = mock(LoginEvent.class);
    when(event.getPlayer()).thenReturn(player);

    new JoinListener(platform).onPlayerConnect(event);

    verify(runtime).onPlayerJoin("JustCain", id, "192.0.2.11");
  }
}
