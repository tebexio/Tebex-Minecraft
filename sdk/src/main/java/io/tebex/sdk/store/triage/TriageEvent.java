package io.tebex.sdk.store.triage;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import io.tebex.sdk.platform.Platform;
import io.tebex.sdk.store.response.ServerInformation;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * A TriageEvent is an indicator of a potential problem that is automatically reported to Tebex.
 *   Information about the Platform at runtime is provided as well as an error message and/or trace
 *   of the error if applicable.
 */
public class TriageEvent {
    private transient final Platform _platform;

    @SerializedName(value = "game_id")
    private String gameId;
    @SerializedName(value = "framework_id")
    private String frameworkId;
    @SerializedName(value = "plugin_version")
    private String pluginVersion;
    @SerializedName(value = "server_ip")
    private String serverIp;
    @SerializedName(value = "error_message")
    private String errorMessage;
    private String trace;
    private Map<String, String> metadata;
    @SerializedName(value = "store_name")
    private String storeName;
    @SerializedName(value = "store_url")
    private String storeUrl;

    private TriageEvent(Platform platform){
        this._platform = platform;
    }

    public static TriageEvent fromPlatform(Platform platform) {
        TriageEvent event = new TriageEvent(platform);

        event.gameId = platform.getTelemetry().getServerSoftware();
        event.frameworkId = platform.getTelemetry().getServerSoftware()
                + " " + platform.getTelemetry().getServerVersion()
                + " " + platform.getTelemetry().getJavaVersion();
        event.pluginVersion = platform.getTelemetry().getPluginVersion();

        event.serverIp = platform.getServerIp();
        if (event.serverIp == null || event.serverIp.isEmpty()) {
            event.serverIp = "0.0.0.0";
        }

        // Assign store info if secret key is set
        if (platform.getStoreSDK().getSecretKey() != null) {
            ServerInformation serverInfo;
            try {
                serverInfo = platform.getStoreSDK().getServerInformation().get();
                event.storeName = serverInfo.getStore().getName();
                event.storeUrl = serverInfo.getStore().getDomain();
            } catch (InterruptedException | ExecutionException e) {
                // store name and info will remain unfilled
            }
        }

        return event;
    }

    public TriageEvent withGameId(String value) {
        this.gameId = value;
        return this;
    }

    public TriageEvent withFrameworkId(String value) {
        this.frameworkId = value;
        return this;
    }

    public TriageEvent withPluginVersion(String value) {
        this.pluginVersion = value;
        return this;
    }

    public TriageEvent withServerIp(String value) {
        this.serverIp = value;
        return this;
    }

    public TriageEvent withErrorMessage(String value) {
        this.errorMessage = value;
        return this;
    }

    public TriageEvent withTrace(String value) {
        this.trace = trace;
        return this;
    }

    public TriageEvent withMetadata(HashMap<String, String> value) {
        this.metadata = value;
        return this;
    }

    public TriageEvent withStoreName(String value) {
        this.storeName = value;
        return this;
    }

    public TriageEvent withStoreUrl(String value) {
        this.storeUrl = value;
        return this;
    }

    public String toJsonString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static TriageEvent fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, TriageEvent.class);
    }

    public void send() {
//        TebexRequest triageEventRequest = _platform.getSDK().request("https://plugin-logs.tebex.io/", false)
//                 .withBody(this.toJsonString(), "POST");
//
//        // Store name is set automatically by fromPlatform
//        if (this.storeName.equals("")) {
//            _platform.debug("No store info while sending triage event, ignoring event");
//            return;
//        }
//
//        // Send the event to plugin logs
//        try {
//            Response triageResponse = triageEventRequest.send();
//
//            if (!triageResponse.isSuccessful()) {
//                _platform.debug("Failed to send triage event!");
//                ResponseBody responseBody = triageResponse.body();
//
//                if (responseBody != null) {
//                    _platform.debug(responseBody.string());
//                } else {
//                    _platform.debug("Empty response from plugin logs when sending triage event");
//                }
//            }
//
//            triageResponse.close();
//        } catch (IOException e) {
//            _platform.debug("Unexpected error sending triage event!");
//            _platform.debug(e.getMessage());
//        }
    }
}
