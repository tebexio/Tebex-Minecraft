package io.tebex.sdk;

import io.tebex.sdk.platform.Platform;

/**
 * The Tebex class serves as the entry point for the Tebex StoreSDK and provides methods to
 * initialise and access the platform instance. The StoreSDK is designed to work with various server
 * platforms, such as Bukkit or Sponge, through the use of the Platform interface.
 */
public class Tebex {
    private static Platform platform;

    /**
     * Private constructor to prevent instantiation of this singleton class.
     */
    public Tebex() {
        throw new UnsupportedOperationException("This is a singleton class and cannot be instantiated");
    }

    /**
     * Initialises the Tebex AnalyticsSDK with the provided platform instance.
     *
     * @param platform The platform instance to initialise the StoreSDK with
     */
    public static void init(Platform platform) {
        Tebex.platform = platform;
    }

    /**
     * Retrieves the currently initialised platform instance.
     *
     * @return The current platform instance, or null if the StoreSDK has not been initialized
     */
    public static Platform get() {
        return platform;
    }
}
