package io.tebex.sdk.analytics.obj;

import com.google.common.collect.Maps;

import java.util.Date;
import java.util.Map;

/**
 * The PlayerEvent class represents a custom event related to a player on the server.
 * It stores information such as the event identifier, origin, time of occurrence, and additional metadata.
 */
public class PlayerEvent {
    private final String id;
    private final String origin;
    private final Date happenedAt;
    private final Map<String, Object> metadata;

    /**
     * Constructs a PlayerEvent with the given identifier and origin.
     *
     * @param id     The event identifier.
     * @param origin The origin of the event.
     */
    public PlayerEvent(String id, String origin) {
        this.id = id.replace(" ", "_");
        this.origin = origin;
        this.happenedAt = new Date();
        this.metadata = Maps.newHashMap();
    }

    /**
     * Constructs a PlayerEvent with the given identifier, origin, and time of occurrence.
     *
     * @param id         The event identifier.
     * @param origin     The origin of the event.
     * @param happenedAt The time of occurrence.
     */
    public PlayerEvent(String id, String origin, Date happenedAt) {
        this.id = id.replace(" ", "_");
        this.origin = origin;
        this.happenedAt = happenedAt;
        this.metadata = Maps.newHashMap();
    }

    /**
     * Adds metadata to the event.
     *
     * @param key   The key for the metadata entry.
     * @param value The value for the metadata entry.
     * @return The updated PlayerEvent instance.
     */
    public PlayerEvent withMetadata(String key, Object value) {
        this.metadata.put(key.replace(" ", "_"), value);
        return this;
    }

    /**
     * Retrieves the event identifier.
     *
     * @return The event identifier.
     */
    public String getId() {
        return id;
    }

    /**
     * Retrieves the event origin.
     *
     * @return The event origin.
     */
    public String getOrigin() {
        return origin;
    }

    /**
     * Retrieves the time the event occurred.
     *
     * @return The time of occurrence.
     */
    public Date getHappenedAt() {
        return happenedAt;
    }

    /**
     * Retrieves the event metadata.
     *
     * @return The event metadata.
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /**
     * Generates a string representation of the PlayerEvent object.
     *
     * @return A string representation of the PlayerEvent object.
     */
    @Override
    public String toString() {
        return "PlayerEvent{" +
                "id='" + id + '\'' +
                ", origin='" + origin + '\'' +
                ", happenedAt=" + happenedAt +
                ", metadata=" + metadata +
                '}';
    }
}