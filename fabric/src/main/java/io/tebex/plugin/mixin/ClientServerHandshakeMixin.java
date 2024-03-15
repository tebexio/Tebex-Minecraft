package io.tebex.plugin.mixin;

import io.tebex.plugin.util.FabricEventHandler;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.server.network.ServerHandshakeNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerHandshakeNetworkHandler.class)
public class ClientServerHandshakeMixin {

    @Inject(method = "onHandshake", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/network/ClientConnection;setPacketListener(Lnet/minecraft/network/listener/PacketListener;)V"))
    public void onClientHandshakeFromNetwork(HandshakeC2SPacket packet, CallbackInfo ci) {
        FabricEventHandler.ON_HANDSHAKE.invoker().onHandshake(packet);
    }

}
