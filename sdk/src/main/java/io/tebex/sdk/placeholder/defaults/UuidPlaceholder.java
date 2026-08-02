package io.tebex.sdk.placeholder.defaults;

import io.tebex.sdk.obj.QueuedPlayer;
import io.tebex.sdk.placeholder.Placeholder;
import io.tebex.sdk.placeholder.PlaceholderManager;
import io.tebex.sdk.util.UUIDUtil;

import static io.tebex.sdk.util.UUIDUtil.EMPTY_UUID;

public class UuidPlaceholder implements Placeholder {
    private final PlaceholderManager placeholderManager;

    public UuidPlaceholder(PlaceholderManager placeholderManager) {
        this.placeholderManager = placeholderManager;
    }

    @Override
    public String handle(QueuedPlayer player, String command) {
        if (player.getUuid() == null || player.getUuid().equals("null") || player.getUuid().equals(EMPTY_UUID.toString())) {

            // Bedrock players don't have UUIDs in the traditional sense.
            // The UUIDs returned by Geyser are hexed versions of the XUID.
            //
            // For example: the XUID 2533274913322943 becomes 900000753B7BF
            //  when converted from DEC to HEX, then Floodgate puts '00000000-0000-0000-000'
            //  in front of it, so the UUID becomes '00000000-0000-0000-0009-00000753B7BF'.
            //
            // This should return the XUID as this is what the Tebex system currently intends
            //  to use, but if they want to swap or someone wants a Floodgate UUID, this is how
            //  they work.

            return placeholderManager.getUniqueIdRegex().matcher(command).replaceAll(player.getXuid() != null ? player.getXuid() : player.getName());
        }
        return placeholderManager.getUniqueIdRegex().matcher(command).replaceAll(UUIDUtil.mojangIdToJavaId(player.getUuid()).toString());
    }
}
