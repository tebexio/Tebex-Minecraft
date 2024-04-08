package io.tebex.sdk.store.placeholder.defaults;

import io.tebex.sdk.store.obj.QueuedPlayer;
import io.tebex.sdk.store.placeholder.Placeholder;
import io.tebex.sdk.store.placeholder.PlaceholderManager;

public class NamePlaceholder implements Placeholder {
    private final PlaceholderManager placeholderManager;

    public NamePlaceholder(PlaceholderManager placeholderManager) {
        this.placeholderManager = placeholderManager;
    }

    @Override
    public String handle(QueuedPlayer player, String command) {
        return placeholderManager.getUsernameRegex().matcher(command).replaceAll(player.getName());
    }
}