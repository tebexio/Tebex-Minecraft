package io.tebex.sdk.analytics.obj;

import com.google.common.collect.Maps;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class Event {
    private final String identifier;
    private final String origin;
    private final Date timestamp;
    private UUID player;
    private final Map<String, Object> metadata;

    public Event(String identifier, String origin) {
        this.identifier = identifier;
        this.origin = origin;
        this.timestamp = new Date();
        this.metadata = Maps.newHashMap();
    }

    public Event(String identifier, String origin, UUID player) {
        this.identifier = identifier;
        this.origin = origin;
        this.timestamp = new Date();
        this.player = player;
        this.metadata = Maps.newHashMap();
    }

    public Event(String identifier, String origin, Date timestamp) {
        this.identifier = identifier;
        this.origin = origin;
        this.timestamp = timestamp;
        this.metadata = Maps.newHashMap();
    }

    public Event(String identifier, String origin, Date timestamp, UUID player) {
        this.identifier = identifier;
        this.origin = origin;
        this.timestamp = timestamp;
        this.player = player;
        this.metadata = Maps.newHashMap();
    }

    public Event(String identifier, String origin, Date timestamp, UUID player, Map<String, Object> metadata) {
        this.identifier = identifier;
        this.origin = origin;
        this.timestamp = timestamp;
        this.player = player;
        this.metadata = metadata;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getOrigin() {
        return origin;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public UUID getPlayer() {
        return player;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Event forPlayer(UUID player) {
        this.player = player;
        return this;
    }

    public Event withMetadata(String key, Object value) {
        metadata.put(key, value);
        return this;
    }
}
