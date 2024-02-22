package io.tebex.plugin.obj;

public interface ServiceManager {
    void load();
    void connect();

    boolean isSetup();
    void setSetup(boolean setup);
}
