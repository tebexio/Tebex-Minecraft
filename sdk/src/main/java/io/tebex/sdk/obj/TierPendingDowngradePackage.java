package io.tebex.sdk.obj;

import com.google.gson.JsonObject;

public class TierPendingDowngradePackage {
    private final int packageId;
    private final String packageName;

    public TierPendingDowngradePackage(int packageId, String packageName) {
        this.packageId = packageId;
        this.packageName = packageName;
    }

    public int getPackageId() {
        return packageId;
    }

    public String getPackageName() {
        return packageName;
    }

    public static TierPendingDowngradePackage fromJsonObject(JsonObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        return new TierPendingDowngradePackage(
                jsonObject.get("id").getAsInt(),
                jsonObject.get("name").getAsString()
        );
    }
}
