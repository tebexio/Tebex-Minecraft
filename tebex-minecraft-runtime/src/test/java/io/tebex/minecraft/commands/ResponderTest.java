package io.tebex.minecraft.commands;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResponderTest {
  @Test
  void formatsTheSameWelcomeSplashForConsoleAndPlayers() {
    Context console = Context.from(true, "Server", null, "tebex", "", null, new String[0]);
    Context player =
        Context.from(
            false,
            "Cain",
            UUID.fromString("bdd4b158-7983-458c-ad6e-cc1a0e20624e"),
            "tebex",
            "",
            null,
            new String[0]);

    assertArrayEquals(
        new String[] {"Welcome to Tebex!", "This server is running version v3.0.0"},
        Responder.formatWelcome(console, "3.0.0"));
    assertArrayEquals(
        new String[] {
          "\u00a7b[Tebex] \u00a7fWelcome to \u00a7bTebex!",
          "\u00a7b[Tebex] \u00a7fThis server is running version \u00a76v3.0.0\u00a7f"
        },
        Responder.formatWelcome(player, "3.0.0"));
  }
}
