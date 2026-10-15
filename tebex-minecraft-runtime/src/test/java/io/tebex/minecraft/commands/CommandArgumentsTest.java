package io.tebex.minecraft.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CommandArgumentsTest {
  @Test
  void joinsUsernameFromRemainingArguments() {
    String[] args = {"123", ".Bedrock", "Player", "Name"};

    assertEquals(".Bedrock Player Name", CommandArguments.join(args, 1));
  }

  @Test
  void removesWrappingQuotesFromJoinedUsername() {
    String[] args = {"123", "\".Bedrock", "Player", "Name\""};

    assertEquals(".Bedrock Player Name", CommandArguments.join(args, 1));
  }

  @Test
  void joinsUsernameBeforeTrailingArguments() {
    String[] args = {".Bedrock", "Player", "Name", "reason", "127.0.0.1"};

    assertEquals(".Bedrock Player Name", CommandArguments.join(args, 0, args.length - 2));
  }

  @Test
  void removesWrappingQuotesBeforeTrailingArguments() {
    String[] args = {"\".Bedrock", "Player", "Name\"", "reason", "127.0.0.1"};

    assertEquals(".Bedrock Player Name", CommandArguments.join(args, 0, args.length - 2));
  }

  @Test
  void returnsEmptyStringForInvalidRange() {
    String[] args = {"lookup"};

    assertEquals("", CommandArguments.join(args, 2));
  }
}
