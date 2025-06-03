package io.tebex.sdk.obj;

import com.google.gson.JsonObject;

public class TierStatus {
    private final int id;
    private final String name;

    public TierStatus(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
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
