package io.tebex.sdk.analytics.response;

import java.util.Date;
import java.util.UUID;

public class ServerInformation {
    private final String name;
    private final UUID uuid;
    private final Date createdAt;

    /**
     * Constructs a ServerInformation instance.
     *
     * @param name The server name.
     * @param uuid The server UUID.
     * @param createdAt The server creation date.
     */
    public ServerInformation(String name, UUID uuid, Date createdAt) {
        this.name = name;
        this.uuid = uuid;
        this.createdAt = createdAt;
    }

    /**
     * Returns the server name.
     *
     * @return The server name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the server UUID.
     *
     * @return The server UUID.
     */
    public UUID getUniqueId() {
        return uuid;
    }

    /**
     * Returns the server creation date.
     *
     * @return The server creation date.
     */
    public Date getCreatedAt() {
        return createdAt;
    }
}