package dev.xhyrom.beamline.network;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

public interface BeamlinePacket {
    void write(FriendlyByteBuf buf);
    void handle(Supplier<NetworkManager.PacketContext> context);
}
