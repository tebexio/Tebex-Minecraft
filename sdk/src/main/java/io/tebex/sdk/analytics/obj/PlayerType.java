package io.tebex.sdk.analytics.obj;

/**
 * The PlayerType enum represents the various player client types that can connect to the server.
 * The Analytics SDK supports both Java and Bedrock Edition players.
 */
public enum PlayerType {
    /**
     * Represents a Java Edition player.
     */
    JAVA,

    /**
     * Represents a Bedrock Edition player.
     */
    BEDROCK
}