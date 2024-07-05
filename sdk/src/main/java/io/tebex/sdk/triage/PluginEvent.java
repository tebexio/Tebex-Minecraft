package io.tebex.sdk.triage;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import io.tebex.sdk.platform.Platform;
import io.tebex.sdk.platform.PlatformTelemetry;
import io.tebex.sdk.request.TebexRequest;
import io.tebex.sdk.request.response.ServerInformation;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

/**
 * A PluginEvent is an indicator of a runtime event that is reported to Tebex. This encapsulates telemetry information
 *  about warnings and errors that occur during runtime.
 */
public class PluginEvent {
    @SerializedName(value = "game_id")
    private String gameId;
    @SerializedName(value = "framework_id")
    private String frameworkId;
    @SerializedName(value = "runtime_version")
    private String runtimeVersion;
    @SerializedName(value = "framework_version")
    private String frameworkVersion;
    @SerializedName(value = "plugin_version")
    private String pluginVersion;
    @SerializedName(value = "store_id")
    private String storeId;
    @SerializedName(value = "store_name")
    private String storeName;
    @SerializedName(value = "server_id")
    private String serverId;
    @SerializedName(value = "event_message")
    private String eventMessage;
    @SerializedName(value = "event_level")
    private EnumEventLevel eventLevel;
    @SerializedName(value = "metadata")
    private Map<String, String> metadata;
    @SerializedName(value = "trace")
    private String trace;

    private final transient Platform _platform;

    public PluginEvent(Platform platform, EnumEventLevel level, String message) {
        this._platform = platform;

        PlatformTelemetry tel = platform.getTelemetry();

        this.gameId = "Minecraft";                                  // always Minecraft
        this.frameworkId = tel.getServerSoftware();                 // name of the platform software, Bukkit, Spigot, etc.
        this.runtimeVersion = "Java " + tel.getJavaVersion();       // version of Java
        this.frameworkVersion = tel.getServerVersion();             // version of Bukkit, Spigot, etc.
        this.pluginVersion = platform.getVersion();
        this.eventLevel = level;
        this.eventMessage = message;
        this.trace = "";
    }

    public PluginEvent onStore(ServerInformation.Store store) {
        return this;
    }
    public PluginEvent onServer(ServerInformation.Server server) {
        return this;
    }
    public PluginEvent withTrace(String trace) {
        this.trace = trace;
        return this;
    }

    public PluginEvent withTrace(Throwable t) {
        StringWriter traceWriter = new StringWriter();
        t.printStackTrace(); // show trace in the console whenever one is provided
        t.printStackTrace(new PrintWriter(traceWriter)); // also write to our var for reporting
        this.trace = traceWriter.toString();
        return this;
    }

    public PluginEvent withTrace() {
        // create a trace to here if no other trace provided
        StringWriter traceWriter = new StringWriter();
        new Throwable().printStackTrace(new PrintWriter(traceWriter)); // also write to our var for reporting
        this.trace = traceWriter.toString();
        return this;
    }

    public String toJsonString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}