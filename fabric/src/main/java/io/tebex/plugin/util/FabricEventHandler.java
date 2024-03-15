package io.tebex.plugin.util;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.text.Text;

import java.net.SocketAddress;

public class FabricEventHandler {
    public static final Event<OnLogin> ON_LOGIN = EventFactory.createArrayBacked(OnLogin.class, callbacks -> (address, profile, reason) -> {
        for (OnLogin callback : callbacks) {
            callback.onLogin(address, profile, reason);
        }
    });

    public static final Event<OnClientHandshake> ON_HANDSHAKE = EventFactory.createArrayBacked(OnClientHandshake.class, callbacks -> packet -> {
        for (OnClientHandshake callback : callbacks) {
            callback.onHandshake(packet);
        }
    });

    @FunctionalInterface
    public interface OnLogin {
        /**
         * Called when a player attempts to login
         *
         * @param address the address of the player
         * @param profile the profile of the player
         * @param reason  the provided kick reason (null if player is permitted to join)
         */
        void onLogin(SocketAddress address, GameProfile profile, Text reason);
    }

    @FunctionalInterface
    public interface OnClientHandshake {
        /**
         * Called when a player attempts to login
         *
         * @param packet Handshake packet
         */
        void onHandshake(HandshakeC2SPacket packet);
    }
}
