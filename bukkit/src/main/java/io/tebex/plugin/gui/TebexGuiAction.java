package io.tebex.plugin.gui;

import org.bukkit.event.Event;

public interface TebexGuiAction<T extends Event>{
    void execute(final T event);
}
