package io.tebex.minecraft.util;

import io.tebex.minecraft.runtime.TebexRuntime;
import io.tebex.model.QueuedCommand;
import io.tebex.model.QueuedPlayer;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Minecraft identity rules applied to SDK models without duplicating wire types. */
public final class PlayerIdentity {
  private static final Pattern TAG =
      Pattern.compile("\\{(username|name|uuid|id)\\}", Pattern.CASE_INSENSITIVE);

  private PlayerIdentity() {}

  public static UUID uuid(String value) {
    if (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null")) return null;
    String text = value.trim();
    if (text.matches("[0-9a-fA-F]{32}"))
      text = text.replaceFirst("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5");
    try {
      UUID id = UUID.fromString(text);
      return id.equals(new UUID(0L, 0L)) ? null : id;
    } catch (IllegalArgumentException invalid) {
      return null;
    }
  }

  public static boolean hasUuid(QueuedPlayer p) {
    return p != null && uuid(p.getUuid()) != null;
  }

  public static boolean hasXuid(QueuedPlayer p) {
    return p != null && p.getXuid() != null && !p.getXuid().trim().isEmpty();
  }

  public static Long xuid(QueuedPlayer p) {
    try {
      return hasXuid(p) ? Long.parseLong(p.getXuid()) : null;
    } catch (NumberFormatException invalid) {
      return null;
    }
  }

  public static String defaultIdentifier(QueuedPlayer p) {
    if (p == null) return "";
    if (hasUuid(p)) return uuid(p.getUuid()).toString();
    return hasXuid(p) ? p.getXuid() : p.getName();
  }

  public static String command(QueuedCommand command, TebexRuntime platform) {
    QueuedPlayer p = command.getPlayer();
    if (p == null) return command.getCommand();
    String id = platform.resolveCommandPlayerId(p);
    Matcher matcher = TAG.matcher(command.getCommand());
    StringBuffer result = new StringBuffer();
    while (matcher.find()) {
      String tag = matcher.group(1);
      String value = tag.equalsIgnoreCase("id") || tag.equalsIgnoreCase("uuid") ? id : p.getName();
      matcher.appendReplacement(
          result, Matcher.quoteReplacement(value == null ? matcher.group() : value));
    }
    matcher.appendTail(result);
    return result.toString();
  }
}
