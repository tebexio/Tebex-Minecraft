package io.tebex.sdk.store.placeholder;

import io.tebex.sdk.store.obj.QueuedPlayer;

public interface Placeholder {
    String handle(QueuedPlayer player, String command);
}