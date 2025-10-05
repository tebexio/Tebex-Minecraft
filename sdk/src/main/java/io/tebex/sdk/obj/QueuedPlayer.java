package io.tebex.sdk.obj;

import com.google.gson.JsonObject;
import lombok.Data;

@Data
public class QueuedPlayer {
    private final int id;
    private final String name;
    private final String uuid;

    /**
     * Constructs a Player instance.
     *
     * @param id The Tebex player ID.
     * @param name The player name.
     * @param uuid The player UUID (raw from API). May be null/invalid for Offline/Geyser stores.
     */
    public QueuedPlayer(int id, String name, String uuid) {
        this.id = id;
        this.name = name;
        this.uuid = uuid;
    }

    public static QueuedPlayer fromJson(JsonObject object) {
        return new QueuedPlayer(
                object.get("id").getAsInt(),
                object.get("name").getAsString(),
                object.has("uuid") && !object.get("uuid").isJsonNull() ? object.get("uuid").getAsString() : null
        );
    }
}
