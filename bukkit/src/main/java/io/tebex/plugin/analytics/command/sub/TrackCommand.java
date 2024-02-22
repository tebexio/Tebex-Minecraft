package io.tebex.plugin.analytics.command.sub;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import io.tebex.plugin.TebexPlugin;
import io.tebex.plugin.obj.SubCommand;
import io.tebex.sdk.analytics.obj.AnalysePlayer;
import io.tebex.sdk.analytics.obj.PlayerEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.stream.Stream;

public class TrackCommand extends SubCommand {
    public TrackCommand(TebexPlugin platform) {
        super(platform, "track", "analytics.track");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            getPlatform().sendMessage(sender, "Usage: /analytics track <player> <event> <metadata>");
            return;
        }

        Player bukkitPlayer = Bukkit.getServer().getPlayer(args[0]);
        if (bukkitPlayer == null) {
            getPlatform().sendMessage(sender, "&cPlayer not found.");
            return;
        }

        AnalysePlayer player = getPlatform().getAnalyticsManager().getPlayer(bukkitPlayer.getUniqueId());
        if (player == null) {
            getPlatform().sendMessage(sender, "&cPlayer not found.");
            return;
        }

        String[] namespace = args[1].split(":", 2);
        String origin = namespace[0];
        String eventName = namespace[1];

        String jsonMetadata = String.join(" ", Stream.of(args).skip(2).toArray(String[]::new));

        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        Map<String, Object> fields = gson.fromJson(jsonMetadata, type);

        PlayerEvent event = new PlayerEvent(eventName, origin);
        for(Map.Entry<String, Object> entry : fields.entrySet()) {
            event.withMetadata(entry.getKey(), entry.getValue());
        }

        player.track(event);
        getPlatform().sendMessage(sender, "Event tracked.");
    }

    @Override
    public String getDescription() {
        return null;
    }
}
