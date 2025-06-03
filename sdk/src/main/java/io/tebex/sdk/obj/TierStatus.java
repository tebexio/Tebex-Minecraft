package io.tebex.sdk.obj;

import com.google.gson.JsonObject;

public class TierStatus {
    private final int id;
    private final String description;

    public TierStatus(int id, String description) {
        this.id = id;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public static TierStatus fromJsonObject(JsonObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        return new TierStatus(
                jsonObject.get("id").getAsInt(),
                jsonObject.get("description").getAsString()
        );
    }
}
