package io.tebex.sdk;

import com.google.gson.JsonObject;
import com.intellectualsites.http.HttpClient;
import com.intellectualsites.http.HttpResponse;
import io.tebex.sdk.analytics.exception.ServerNotFoundException;
import io.tebex.sdk.analytics.exception.ServerNotSetupException;
import io.tebex.sdk.analytics.obj.AnalysePlayer;
import io.tebex.sdk.analytics.response.AnalyseLeaderboard;
import io.tebex.sdk.analytics.response.PlayerProfile;
import io.tebex.sdk.analytics.response.PluginInformation;
import io.tebex.sdk.analytics.response.ServerInformation;
import io.tebex.sdk.exception.NotFoundException;
import io.tebex.sdk.exception.RateLimitException;
import io.tebex.sdk.platform.Platform;
import io.tebex.sdk.platform.PlatformType;
import io.tebex.sdk.util.StringUtil;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static io.tebex.sdk.HttpSdkBuilder.GSON;

/**
 * The main AnalyticsSDK class for interacting with the Analytics API.
 */
public class AnalyticsSDK {
    private final HttpClient HTTP_CLIENT;

    private final int API_VERSION = 1;
    private final String SECRET_KEY_HEADER = "X-Server-Token";

    private final Platform platform;
    private String serverToken;

    /**
     * Constructs a new AnalyticsSDK instance with the specified platform and server token.
     *
     * @param platform    The platform on which the AnalyticsSDK is running.
     * @param serverToken The server token for authentication.
     */
    public AnalyticsSDK(Platform platform, String serverToken) {
        this.platform = platform;
        this.serverToken = serverToken;

        HttpSdkBuilder httpSdkBuilder = new HttpSdkBuilder(String.format("https://analytics.tebex.io/api/v%d", API_VERSION));
        this.HTTP_CLIENT = httpSdkBuilder.build();
    }

    private void handleRequestErrors(HttpResponse req) {
        if (req.getStatusCode() == 404) {
            throw new CompletionException(new ServerNotFoundException());
        } else if (req.getStatusCode() == 429) {
            throw new CompletionException(new RateLimitException("You are being rate limited."));
        } else if (req.getStatusCode() != 200) {
            throw new CompletionException(new IOException("Unexpected status code (" + req.getStatusCode() + ")"));
        }
    }

    /**
     * Retrieves the latest plugin information.
     *
     * @param platformType The platform type for which to retrieve the plugin information.
     * @return A CompletableFuture that contains the PluginInformation object.
     */
    public CompletableFuture<PluginInformation> getPluginVersion(PlatformType platformType) {
        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.get("/plugin")
                    .withHeader(SECRET_KEY_HEADER, serverToken)
                    .withHeader("User-Agent", "Tebex-AnalyticsSDK")
                    .withHeader("Content-Type", "application/json")
                    .onStatus(200, req -> {})
                    .onRemaining(this::handleRequestErrors)
                    .execute();

            if(response == null) {
                throw new CompletionException(new IOException("Failed to retrieve plugin information"));
            }

            JsonObject body = response.getResponseEntity(JsonObject.class);
            JsonObject versionData = body.get("version").getAsJsonObject();
            JsonObject assetData = body.get("assets").getAsJsonObject();

            return new PluginInformation(
                    versionData.get("name").getAsString(),
                    versionData.get("incremental").getAsInt(),
                    assetData.get(platformType.name().toLowerCase()).getAsString()
            );
        });
    }

    /**
     * Retrieves information about the server.
     *
     * @return A CompletableFuture that contains the ServerInformation object.
     */
    public CompletableFuture<ServerInformation> getServerInformation() {
        if (getServerToken() == null) {
            CompletableFuture<ServerInformation> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.get("/server")
                    .withHeader(SECRET_KEY_HEADER, serverToken)
                    .withHeader("User-Agent", "Tebex-AnalyticsSDK")
                    .withHeader("Content-Type", "application/json")
                    .onStatus(200, req -> {})
                    .onRemaining(this::handleRequestErrors)
                    .execute();

            if(response == null) {
                throw new CompletionException(new IOException("Failed to retrieve server information"));
            }

            JsonObject body = response.getResponseEntity(JsonObject.class);
            return GSON.fromJson(body.get("data"), ServerInformation.class);
        });
    }

    /**
     * Sends a player session to the Analytics API for tracking.
     *
     * @param player The AnalysePlayer object representing the player to be tracked.
     * @return A CompletableFuture that indicates whether the operation was successful.
     */
    public CompletableFuture<Boolean> trackPlayerSession(AnalysePlayer player) {
        if (getServerToken() == null) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(new NotFoundException());
            return future;
        }

        if (! platform.isAnalyticsSetup()) {
            platform.debug("Skipped tracking player session for " + player.getName() + " as Analytics isn't setup.");
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.complete(false);
            return future;
        }

        if(platform.isPlayerExcluded(player.getUniqueId())) {
            platform.debug("Skipped tracking player session for " + player.getName() + " as they are excluded.");
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.complete(false);
            return future;
        }

        player.logout();
        platform.debug("Sending payload: " + GSON.toJson(player));

        platform.debug("Tracking player session for " + player.getName() + "..");
        platform.debug(" - UUID: " + player.getUniqueId());
        platform.debug(" - Type: " + player.getType());
        platform.debug(" - Played for: " + player.getDurationInSeconds() + "s");
        platform.debug(" - IP: " + player.getIpAddress());
        platform.debug(" - Joined at: " + player.getJoinedAt());
        platform.debug(" - First joined at: " + player.getFirstJoinedAt());

        if(! player.getStatistics().isEmpty()) {
            platform.debug(" - Statistics:");
            player.getStatistics().forEach((key, value) -> platform.debug("   - %" + key + "%: " + value));
        } else {
            platform.debug(" - No statistics to track.");
        }

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.get("/server/sessions")
                    .withHeader(SECRET_KEY_HEADER, serverToken)
                    .withInput(() -> GSON.toJson(player))
                    .onStatus(200, req -> {})
                    .onRemaining(this::handleRequestErrors)
                    .execute();

            if(response == null) {
                throw new CompletionException(new IOException("Failed to track player session"));
            }

            System.out.println(response.getResponseEntity(String.class));
            JsonObject body = response.getResponseEntity(JsonObject.class);

            return body.get("success").getAsBoolean();
        });
    }

    /**
     * Sends a setup completion request to the Analytics API.
     *
     * @return A CompletableFuture that indicates whether the operation was successful.
     */
    public CompletableFuture<Boolean> completeServerSetup() {
        if (getServerToken() == null) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.get("/server/setup")
                    .withHeader(SECRET_KEY_HEADER, serverToken)
                    .onStatus(200, req -> {})
                    .onRemaining(this::handleRequestErrors)
                    .execute();

            if(response == null) {
                throw new CompletionException(new IOException("Failed to complete server setup"));
            }

            JsonObject body = response.getResponseEntity(JsonObject.class);
            return body.get("success").getAsBoolean();
        });
    }

    /**
     * Sends the current player count to the Analytics API in the form of a heartbeat.
     *
     * @param playerCount The number of players currently online.
     * @return A CompletableFuture that indicates whether the operation was successful.
     */
    public CompletableFuture<Boolean> trackHeartbeat(int playerCount) {
        if (getServerToken() == null) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        JsonObject body = new JsonObject();
        body.addProperty("players", playerCount);

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.post("/server/heartbeat")
                    .withHeader(SECRET_KEY_HEADER, serverToken)
                    .withInput(() -> GSON.toJson(body))
                    .onStatus(200, req -> {})
                    .onRemaining(this::handleRequestErrors)
                    .execute();

            if(response == null) {
                throw new CompletionException(new IOException("Failed to track heartbeat"));
            }

            JsonObject responseBody = response.getResponseEntity(JsonObject.class);

            return responseBody.get("success").getAsBoolean();
        });
    }

    /**
     * Sends the current server telemetry to the Analytics API.
     *
     * @return A CompletableFuture that indicates whether the operation was successful.
     */
    public CompletableFuture<Boolean> sendTelemetry() {
        if (getServerToken() == null) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.post("/server/telemetry")
                    .withHeader(SECRET_KEY_HEADER, serverToken)
                    .withInput(() -> GSON.toJson(platform.getTelemetry()))
                    .onStatus(200, req -> {})
                    .onRemaining(this::handleRequestErrors)
                    .execute();

            if(response == null) {
                throw new CompletionException(new IOException("Failed to send telemetry"));
            }

            JsonObject responseBody = response.getResponseEntity(JsonObject.class);
            return responseBody.get("success").getAsBoolean();
        });
    }

    /**
     * Get the country code of a specific IP address.
     *
     * @param ip The IP address
     * @return CompletableFuture containing the country code
     * @deprecated This method is deprecated and may be removed in future versions
     */
    @Deprecated
    public CompletableFuture<String> getCountryFromIp(String ip) {
        if (getServerToken() == null) {
            CompletableFuture<String> future = new CompletableFuture<>();
            future.completeExceptionally(new ServerNotSetupException());
            return future;
        }

        return CompletableFuture.supplyAsync(() -> {
            final HttpResponse response = this.HTTP_CLIENT.get("/ip/" + ip)
                    .withHeader(SECRET_KEY_HEADER, serverToken)
                    .onStatus(200, req -> {})
                    .onRemaining(this::handleRequestErrors)
                    .execute();

            if(response == null) {
                throw new CompletionException(new IOException("Failed to retrieve country from IP"));
            }

            JsonObject jsonObject = response.getResponseEntity(JsonObject.class);

            return jsonObject.get("success").getAsBoolean() ? jsonObject.get("country_code").getAsString() : null;
        });
    }

    /**
     * Get the server token associated with this AnalyticsSDK instance.
     *
     * @return The server token as a String
     */
    public String getServerToken() {
        return serverToken;
    }

    /**
     * Set the server token for this AnalyticsSDK instance.
     *
     * @param serverToken The server token as a String
     */
    public void setServerToken(String serverToken) {
        this.serverToken = serverToken;
    }
}