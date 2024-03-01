package io.tebex.plugin.analytics.command.sub;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.sdk.analytics.obj.AnalysePlayer;
import io.tebex.sdk.analytics.obj.PlayerEvent;
import io.tebex.sdk.platform.PlatformLang;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.stream.Stream;

public class TrackCommand extends SubCommand {
    public TrackCommand(TebexPlugin platform) {
        super(platform, "track", "tebex.track");
    }

    @Override
    public void execute(CommandContext<ServerCommandSource> context) {
        final ServerCommandSource sender = context.getSource();
        final TebexPlugin platform = getPlatform();

        if (! platform.isAnalyticsSetup()) {
            platform.sendMessage(sender, PlatformLang.NOT_CONNECTED_TO_ANALYTICS.get());
            return;
        }

        ServerPlayerEntity playerEntity;

        try {
            playerEntity = EntityArgumentType.getPlayer(context, "player");
        } catch (CommandSyntaxException e) {
            getPlatform().sendMessage(sender, PlatformLang.PLAYER_NOT_FOUND.get());
            return;
        }

        AnalysePlayer player = getPlatform().getAnalyticsManager().getPlayer(playerEntity.getUuid());
        this.getPlatform().sendMessage(sender, "Player: " + playerEntity.getName().asString() + " UUID: " + playerEntity.getUuid());
        if (player == null) {
            getPlatform().sendMessage(sender, PlatformLang.PLAYER_NOT_FOUND.get());
            return;
        }

        String eventArg = StringArgumentType.getString(context, "event");
        String[] namespace = eventArg.split(":", 2);

        String origin = namespace[0];
        String eventName = namespace.length > 1 ? namespace[1] : "Unknown";

        String metadataArg = StringArgumentType.getString(context, "metadata");
        String jsonMetadata = String.join(" ", Stream.of(metadataArg).toArray(String[]::new));

        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> fields = gson.fromJson(jsonMetadata, type);

        PlayerEvent event = new PlayerEvent(eventName, origin);
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            event.withMetadata(entry.getKey(), entry.getValue());
        }

        player.track(event);
        getPlatform().sendMessage(sender, PlatformLang.EVENT_TRACKED.get());
    }

    @Override
    public String getDescription() {
        return "Track an event for a player.";
    }

    @Override
    public String getUsage() {
        return "<player> <origin:eventName> [metadata]";
    }
}
