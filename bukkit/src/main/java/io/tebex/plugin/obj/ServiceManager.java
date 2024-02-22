package io.tebex.plugin.obj;

public interface ServiceManager {
    void init();
    void connect();

    boolean isSetup();
    void setSetup(boolean setup);
}
