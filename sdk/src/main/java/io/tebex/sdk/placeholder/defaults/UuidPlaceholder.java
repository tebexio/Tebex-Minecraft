package io.tebex.sdk.placeholder.defaults;

import io.tebex.sdk.obj.QueuedPlayer;
import io.tebex.sdk.placeholder.Placeholder;
import io.tebex.sdk.placeholder.PlaceholderManager;
import io.tebex.sdk.util.UUIDUtil;
import java.util.UUID;

public class UuidPlaceholder implements Placeholder {
    private final PlaceholderManager placeholderManager;

    public UuidPlaceholder(PlaceholderManager placeholderManager) {
        this.placeholderManager = placeholderManager;
    }

    @Override
    public String handle(QueuedPlayer player, String command) {
        String mojangId = player.getUuid();
        if (mojangId == null || mojangId.isEmpty() || mojangId.equalsIgnoreCase("null")) {
            return placeholderManager.getUniqueIdRegex().matcher(command).replaceAll(player.getName());
        }

        UUID uuid = UUIDUtil.mojangIdToJavaId(mojangId);
        if (uuid == null) {
            return placeholderManager.getUniqueIdRegex().matcher(command).replaceAll(player.getName());
        }

        return placeholderManager.getUniqueIdRegex().matcher(command).replaceAll(uuid.toString());
    }
}
