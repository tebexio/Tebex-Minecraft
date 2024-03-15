package io.tebex.sdk.platform;

/**
 * The PlatformType enum represents the different server platforms supported by the Tebex SDK.
 * The current supported platforms include Bukkit, BungeeCord, Velocity, and Fabric.
 */
public enum PlatformType {
    /**
     * Represents the Bukkit server platform.
     */
    BUKKIT("Spigot/Paper"),

    /**
     * Represents the BungeeCord server platform.
     */
    BUNGEECORD("BungeeCord"),

    /**
     * Represents the Velocity server platform.
     */
    VELOCITY("Velocity"),

    /**
     * Represents the Fabric server platform.
     */
    FABRIC("Fabric");

    private final String name;

    PlatformType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}