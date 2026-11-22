package io.tebex.plugin.command.sub;

import com.velocitypowered.api.command.CommandSource;
import io.tebex.plugin.TebexVelocityPlugin;
import io.tebex.plugin.command.SubCommand;
import io.tebex.sdk.platform.config.IPlatformConfig;
import io.tebex.sdk.request.TebexRequest;

import static net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection;

public class SecretCommand extends SubCommand {
    public SecretCommand(TebexVelocityPlugin platform) {
        super(platform, "secret", "tebex.setup");
    }

    @Override
    public void execute(CommandSource sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(legacySection().deserialize("§b[Tebex] §7Usage: §f/tebex secret <key>"));
            return;
        }

        String candidateKey = args[0];
        TebexVelocityPlugin platform = getPlatform();

        try {
            TebexRequest.validateSecretKey(candidateKey);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(legacySection().deserialize("§b[Tebex] §c" + e.getMessage()));
            return;
        }

        platform.getSDK().getServerInformation(candidateKey).whenComplete((serverInformation, error) -> {
            if (error != null) {
                sender.sendMessage(legacySection().deserialize("§b[Tebex] §cCould not verify the secret key. Check the key and your connection to Tebex."));
                return;
            }

            IPlatformConfig config = platform.getPlatformConfig();
            config.setSecretKey(candidateKey);
            platform.saveConfig(config);
            platform.initStore();

            sender.sendMessage(legacySection().deserialize("§b[Tebex] §7Verified the key for §b" + serverInformation.getServer().getName() + "§7."));
        });
    }

    @Override
    public String getDescription() {
        return "Connects to your Tebex store.";
    }

    @Override
    public String getUsage() {
        return "<key>";
    }
}
