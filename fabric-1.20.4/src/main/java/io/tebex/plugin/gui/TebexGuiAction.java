package io.tebex.plugin.gui;

public interface TebexGuiAction<T extends TebexBuyScreenHandler>{
    void execute(final T event);
}
