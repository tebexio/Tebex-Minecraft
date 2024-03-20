package io.tebex.plugin.command;

import com.velocitypowered.api.command.CommandSource;
import io.tebex.plugin.TebexPlugin;
import net.kyori.adventure.text.Component;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

public abstract class SubCommand {
    private final TebexPlugin platform;
    private final String name;
    private final String permission;

    public SubCommand(TebexPlugin platform, String name, String permission) {
        this.platform = platform;
        this.name = name;
        this.permission = permission;
    }

    public abstract void execute(final CommandSource sender, final String[] args);

    public TebexPlugin getPlatform() {
        return platform;
    }

    public String getName() {
        return name;
    }

    public String getPermission() {
        return permission;
    }

    public abstract String getDescription();

    public String getUsage() {
        return "";
    }

    protected Component getInvalidUsageMessage() {
        return legacySection().deserialize("§b[Tebex] §7Invalid command usage. Use /tebex " + this.getName() + " " + getUsage());
    }
}