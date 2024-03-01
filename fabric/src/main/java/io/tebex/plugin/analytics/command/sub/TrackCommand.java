package io.tebex.plugin.analytics.command.sub;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.dejvokep.boostedyaml.YamlDocument;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.sdk.analytics.SDK;
import io.tebex.sdk.analytics.obj.AnalysePlayer;
import io.tebex.sdk.analytics.obj.PlayerEvent;
import io.tebex.sdk.exception.NotFoundException;
import io.tebex.sdk.platform.PlatformLang;
import io.tebex.sdk.platform.config.ServerPlatformConfig;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.stream.Stream;

public class TrackCommand extends SubCommand {
    public TrackCommand(TebexPlugin platform) {
        super(platform, "track", "tebex.track");
    }

    @Override
    public void execute(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity playerEntity;

        try {
            playerEntity = EntityArgumentType.getPlayer(context, "player");
        } catch (CommandSyntaxException e) {
            getPlatform().sendMessage(source, PlatformLang.PLAYER_NOT_FOUND.get());
            return;
        }

        AnalysePlayer player = getPlatform().getAnalyticsManager().getPlayer(playerEntity.getUuid());
        if (player == null) {
            getPlatform().sendMessage(source, PlatformLang.PLAYER_NOT_FOUND.get());
            return;
        }

        String eventArg = StringArgumentType.getString(context, "event");
        String[] namespace = eventArg.split(":", 2);
        if (namespace.length < 2) {
            getPlatform().sendMessage(source, "Event argument must be in the format 'origin:eventName'.");
            return;
        }

        String origin = namespace[0];
        String eventName = namespace[1];

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
        getPlatform().sendMessage(source, PlatformLang.EVENT_TRACKED.get());
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
