package io.tebex.plugin.compat;

import com.cryptomorin.xseries.XMaterial;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.Material;

public class MaterialUtil {
  private static final Pattern X_SERIES_COMPATIBLE_VERSION = Pattern.compile("MC: \\d\\.\\d+");

  public static Optional<Material> fromString(String material) {
    if (material == null) {
      return Optional.empty();
    }

    String input = material.trim();
    if (input.isEmpty()) {
      return Optional.empty();
    }

    Optional<Material> matchedMaterial = matchBukkitMaterial(input);
    if (matchedMaterial.isPresent()) {
      return matchedMaterial;
    }

    matchedMaterial = matchLegacyId(input);
    if (matchedMaterial.isPresent()) {
      return matchedMaterial;
    }

    if (canUseXSeriesVersionParser()) {
      return matchXSeries(input);
    }

    return Optional.empty();
  }

  private static Optional<Material> matchBukkitMaterial(String material) {
    Material matched = matchMaterial(material);
    if (matched != null) {
      return Optional.of(matched);
    }

    String name = material;
    int namespaceSeparator = material.indexOf(':');
    if (namespaceSeparator != -1
        && material.substring(0, namespaceSeparator).equalsIgnoreCase("minecraft")) {
      name = material.substring(namespaceSeparator + 1);
    }

    String formattedName =
        name.trim().replace('-', '_').replace(' ', '_').toUpperCase(Locale.ENGLISH);

    matched = Material.getMaterial(formattedName);
    if (matched != null) {
      return Optional.of(matched);
    }

    matched = matchMaterial(formattedName);
    return Optional.ofNullable(matched);
  }

  private static Optional<Material> matchLegacyId(String material) {
    String idPart = material;
    String dataPart = "0";

    int dataSeparator = material.indexOf(':');
    if (dataSeparator != -1) {
      idPart = material.substring(0, dataSeparator);
      dataPart = material.substring(dataSeparator + 1);
    }

    try {
      int id = Integer.parseInt(idPart.trim());
      byte data = Byte.parseByte(dataPart.trim());

      if (canUseXSeriesVersionParser()) {
        Optional<Material> xSeriesMaterial = matchXSeries(id, data);
        if (xSeriesMaterial.isPresent()) {
          return xSeriesMaterial;
        }
      }

      Optional<Material> knownLegacyMaterial = matchKnownLegacyId(id, data);
      if (knownLegacyMaterial.isPresent()) {
        return knownLegacyMaterial;
      }

      return Optional.ofNullable(getMaterialByLegacyId(id));
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  private static Material matchMaterial(String material) {
    try {
      Method matchMaterial = Material.class.getMethod("matchMaterial", String.class);
      return (Material) matchMaterial.invoke(null, material);
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static Material getMaterialByLegacyId(int id) {
    try {
      Method getMaterial = Material.class.getMethod("getMaterial", int.class);
      return (Material) getMaterial.invoke(null, id);
    } catch (Throwable ignored) {
      return null;
    }
  }

  private static Optional<Material> matchKnownLegacyId(int id, byte data) {
    if (id == 35) {
      return matchBukkitMaterial(colorName(data) + "_WOOL");
    }
    if (id == 95) {
      return matchBukkitMaterial(colorName(data) + "_STAINED_GLASS");
    }
    if (id == 160) {
      return matchBukkitMaterial(colorName(data) + "_STAINED_GLASS_PANE");
    }
    if (id == 171) {
      return matchBukkitMaterial(colorName(data) + "_CARPET");
    }
    if (id == 159) {
      return matchBukkitMaterial(colorName(data) + "_TERRACOTTA");
    }

    switch (id) {
      case 1:
        return matchBukkitMaterial("STONE");
      case 2:
        return matchBukkitMaterial("GRASS_BLOCK");
      case 3:
        return matchBukkitMaterial("DIRT");
      case 4:
        return matchBukkitMaterial("COBBLESTONE");
      case 5:
        return matchBukkitMaterial("OAK_PLANKS");
      case 17:
        return matchBukkitMaterial("OAK_LOG");
      case 41:
        return matchBukkitMaterial("GOLD_BLOCK");
      case 42:
        return matchBukkitMaterial("IRON_BLOCK");
      case 46:
        return matchBukkitMaterial("TNT");
      case 47:
        return matchBukkitMaterial("BOOKSHELF");
      case 49:
        return matchBukkitMaterial("OBSIDIAN");
      case 54:
        return matchBukkitMaterial("CHEST");
      case 57:
        return matchBukkitMaterial("DIAMOND_BLOCK");
      case 58:
        return matchBukkitMaterial("CRAFTING_TABLE");
      case 61:
        return matchBukkitMaterial("FURNACE");
      case 79:
        return matchBukkitMaterial("ICE");
      case 80:
        return matchBukkitMaterial("SNOW_BLOCK");
      case 89:
        return matchBukkitMaterial("GLOWSTONE");
      case 133:
        return matchBukkitMaterial("EMERALD_BLOCK");
      case 138:
        return matchBukkitMaterial("BEACON");
      case 145:
        return matchBukkitMaterial("ANVIL");
      case 152:
        return matchBukkitMaterial("REDSTONE_BLOCK");
      case 173:
        return matchBukkitMaterial("COAL_BLOCK");
      case 264:
        return matchBukkitMaterial("DIAMOND");
      case 265:
        return matchBukkitMaterial("IRON_INGOT");
      case 266:
        return matchBukkitMaterial("GOLD_INGOT");
      case 276:
        return matchBukkitMaterial("DIAMOND_SWORD");
      case 277:
        return matchBukkitMaterial("DIAMOND_SHOVEL");
      case 278:
        return matchBukkitMaterial("DIAMOND_PICKAXE");
      case 279:
        return matchBukkitMaterial("DIAMOND_AXE");
      case 310:
        return matchBukkitMaterial("DIAMOND_HELMET");
      case 311:
        return matchBukkitMaterial("DIAMOND_CHESTPLATE");
      case 312:
        return matchBukkitMaterial("DIAMOND_LEGGINGS");
      case 313:
        return matchBukkitMaterial("DIAMOND_BOOTS");
      case 322:
        return matchBukkitMaterial("GOLDEN_APPLE");
      case 339:
        return matchBukkitMaterial("PAPER");
      case 340:
        return matchBukkitMaterial("BOOK");
      case 345:
        return matchBukkitMaterial("COMPASS");
      case 388:
        return matchBukkitMaterial("EMERALD");
      default:
        return Optional.empty();
    }
  }

  private static String colorName(byte data) {
    switch (data) {
      case 1:
        return "ORANGE";
      case 2:
        return "MAGENTA";
      case 3:
        return "LIGHT_BLUE";
      case 4:
        return "YELLOW";
      case 5:
        return "LIME";
      case 6:
        return "PINK";
      case 7:
        return "GRAY";
      case 8:
        return "LIGHT_GRAY";
      case 9:
        return "CYAN";
      case 10:
        return "PURPLE";
      case 11:
        return "BLUE";
      case 12:
        return "BROWN";
      case 13:
        return "GREEN";
      case 14:
        return "RED";
      case 15:
        return "BLACK";
      default:
        return "WHITE";
    }
  }

  private static boolean canUseXSeriesVersionParser() {
    try {
      String version = Bukkit.getVersion();
      return version != null && X_SERIES_COMPATIBLE_VERSION.matcher(version).find();
    } catch (Throwable ignored) {
      return false;
    }
  }

  private static Optional<Material> matchXSeries(String material) {
    try {
      return XMaterial.matchXMaterial(material).map(XMaterial::parseMaterial);
    } catch (Throwable ignored) {
      return Optional.empty();
    }
  }

  private static Optional<Material> matchXSeries(int id, byte data) {
    try {
      return XMaterial.matchXMaterial(id, data).map(XMaterial::parseMaterial);
    } catch (Throwable ignored) {
      return Optional.empty();
    }
  }
}
