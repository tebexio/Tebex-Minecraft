package io.tebex.minecraft.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import io.tebex.http.PluginApi;
import io.tebex.model.ServerInformation;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class SdkSessionTest {
  @Test
  void forwardsItsCredentialThroughTheOfficialSdkTransportAndModels() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    AtomicReference<String> receivedKey = new AtomicReference<>();
    server.createContext(
        "/information",
        exchange -> {
          receivedKey.set(exchange.getRequestHeaders().getFirst("X-Tebex-Secret"));
          byte[] body =
              "{\"account\":{\"id\":1,\"name\":\"Fixture\",\"currency\":{\"iso_4217\":\"USD\"}},\"server\":{\"id\":2},\"public_token\":\"public\"}"
                  .getBytes(StandardCharsets.UTF_8);
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();
    try {
      SdkSession session =
          new SdkSession(
              "test-secret", new PluginApi("http://127.0.0.1:" + server.getAddress().getPort()));

      ServerInformation information =
          session
              .request((api, secret) -> api.getServerInformation(secret))
              .get(5, TimeUnit.SECONDS);

      assertEquals("test-secret", receivedKey.get());
      assertEquals("Fixture", information.getAccount().getName());
      assertNotNull(information.getServer());
    } finally {
      server.stop(0);
    }
  }

  @Test
  void retiredSessionDoesNotStartAnotherSdkRequest() {
    PluginApi api = org.mockito.Mockito.mock(PluginApi.class);
    SdkSession session = new SdkSession("old", api);
    session.close();

    CompletableFuture<?> result = session.request((client, secret) -> client.getDuePlayers(secret));

    assertTrue(result.isCompletedExceptionally());
    org.mockito.Mockito.verifyNoInteractions(api);
  }

  @Test
  void retiredSessionRejectsLateSdkResponse() {
    CompletableFuture<ServerInformation> response = new CompletableFuture<>();
    PluginApi api = org.mockito.Mockito.mock(PluginApi.class);
    org.mockito.Mockito.when(api.getServerInformation("old")).thenReturn(response);
    SdkSession session = new SdkSession("old", api);

    CompletableFuture<ServerInformation> result =
        session.request((client, secret) -> client.getServerInformation(secret));
    session.close();
    response.complete(null);

    assertTrue(result.isCompletedExceptionally());
  }
}
