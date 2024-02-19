package io.tebex.plugin.service;

public interface ServiceManager {
    void load();
    void connect();

    boolean isSetup();
    void setSetup(boolean setup);
}
