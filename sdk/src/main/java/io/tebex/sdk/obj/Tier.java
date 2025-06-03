package io.tebex.sdk.obj;

import com.google.gson.JsonObject;

import java.time.ZonedDateTime;

public class Tier {
    private final int id;
    private final ZonedDateTime createdAt;
    private final String usernameId;
    private final Package tierPackage;
    private final boolean active;
    private final String recurringPaymentReference;
    private final ZonedDateTime nextPaymentDate;
    private final TierStatus status;
    private final TierPendingDowngradePackage downgradePackage;

    public Tier(int id, ZonedDateTime createdAt, String usernameId, Package tierPackage, boolean active, String recurringPaymentReference, ZonedDateTime nextPaymentDate, TierStatus status, TierPendingDowngradePackage downgradePackage) {
        this.id = id;
        this.createdAt = createdAt;
        this.usernameId = usernameId;
        this.tierPackage = tierPackage;
        this.active = active;
        this.recurringPaymentReference = recurringPaymentReference;
        this.nextPaymentDate = nextPaymentDate;
        this.status = status;
        this.downgradePackage = downgradePackage;
    }

    public int getId() {
        return id;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public String getUsernameId() {
        return usernameId;
    }

    public Package getPackage() {
        return tierPackage;
    }

    public boolean isActive() {
        return active;
    }

    public String getRecurringPaymentReference() {
        return recurringPaymentReference;
    }

    public ZonedDateTime getNextPaymentDate() {
        return nextPaymentDate;
    }

    public TierStatus getStatus() {
        return status;
    }

    public TierPendingDowngradePackage getDowngradePackage() {
        return downgradePackage;
    }

    public static Tier fromJsonObject(JsonObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }

        return new Tier(
                jsonObject.get("id").getAsInt(),
                ZonedDateTime.parse(jsonObject.get("created_at").getAsString()),
                jsonObject.get("username_id").getAsString(),
                Package.fromJsonObject(jsonObject.get("package").getAsJsonObject()),
                jsonObject.get("active").getAsBoolean(),
                jsonObject.get("recurring_payment_reference").getAsString(),
                ZonedDateTime.parse(jsonObject.get("next_payment_date").getAsString()),
                TierStatus.fromJsonObject(jsonObject.get("status").getAsJsonObject()),
                TierPendingDowngradePackage.fromJsonObject(jsonObject.get("downgrade_package").getAsJsonObject())
        );
    }

    @Override
    public String toString() {
        return "Tier{" +
                "id=" + id +
                ", createdAt=" + createdAt +
                ", usernameId='" + usernameId + '\'' +
                ", tierPackage=" + tierPackage +
                ", active=" + active +
                ", recurringPaymentReference='" + recurringPaymentReference + '\'' +
                ", nextPaymentDate=" + nextPaymentDate +
                ", status=" + status +
                ", downgradePackage=" + downgradePackage +
                '}';
    }
}
