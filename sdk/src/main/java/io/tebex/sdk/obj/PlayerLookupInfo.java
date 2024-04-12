package io.tebex.sdk.obj;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayerLookupInfo {
    public Player player;
    public int banCount;
    public int chargebackRate;
    public List<Payment> payments;
    public Map<String, Double> purchaseTotals;

    // Getters
    public Player getLookupPlayer() {
        return player;
    }

    public int getBanCount() {
        return banCount;
    }

    public int getChargebackRate() {
        return chargebackRate;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public Map<String, Double> getPurchaseTotals() {
        return purchaseTotals;
    }

    // Nested Player class
    public static class Player {
        public String id;
        public String username;
        public String meta;
        public int pluginUsernameId;

        // Getters
        public String getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public String getMeta() {
            return meta;
        }

        public int getPluginUsernameId() {
            return pluginUsernameId;
        }
    }

    // Nested Payment class
    public static class Payment {
        public String txnId;
        public long time;
        public double price;
        public String currency;
        public int status;

        // Getters
        public String getTxnId() {
            return txnId;
        }

        public long getTime() {
            return time;
        }

        public double getPrice() {
            return price;
        }

        public String getCurrency() {
            return currency;
        }

        public int getStatus() {
            return status;
        }
    }

    public static PlayerLookupInfo fromJsonObject(JsonObject jsonObject) {
        // Parse Player object
        JsonObject playerJson = jsonObject.get("player").getAsJsonObject();
        Player player = new Player();
        player.id = playerJson.get("id").getAsString();
        player.username = playerJson.get("username").getAsString();
        player.meta = playerJson.get("meta").getAsString();
        player.pluginUsernameId = playerJson.get("plugin_username_id").getAsInt();

        // Parse banCount and chargebackRate
        int banCount = jsonObject.get("banCount").getAsInt();
        int chargebackRate = jsonObject.get("chargebackRate").getAsInt();

        // Parse Payments array
        JsonArray paymentsJsonArray = jsonObject.get("payments").getAsJsonArray();
        List<Payment> payments = new ArrayList<>();
        for (JsonElement paymentElement : paymentsJsonArray) {
            JsonObject paymentJson = paymentElement.getAsJsonObject();
            Payment payment = new Payment();
            payment.txnId = paymentJson.get("txn_id").getAsString();
            payment.time = paymentJson.get("time").getAsLong();
            payment.price = paymentJson.get("price").getAsDouble();
            payment.currency = paymentJson.get("currency").getAsString();
            payment.status = paymentJson.get("status").getAsInt();
            payments.add(payment);
        }

        // Construct and return the PlayerLookupInfo object
        PlayerLookupInfo playerLookupInfo = new PlayerLookupInfo();
        playerLookupInfo.player = player;
        playerLookupInfo.banCount = banCount;
        playerLookupInfo.chargebackRate = chargebackRate;
        playerLookupInfo.payments = payments;

        // Parse purchaseTotals map
        JsonElement purchaseTotalsJson = jsonObject.get("purchaseTotals");
        if (purchaseTotalsJson.isJsonObject()) { // empty
            JsonObject purchaseTotalsObj = purchaseTotalsJson.getAsJsonObject();
            Map<String, Double> purchaseTotals = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : purchaseTotalsObj.entrySet()) {
                purchaseTotals.put(entry.getKey(), entry.getValue().getAsDouble());
            }
            playerLookupInfo.purchaseTotals = purchaseTotals;
        }

        return playerLookupInfo;
    }
}
