package dev.xhyrom.beamline.registry;

import dev.architectury.networking.NetworkChannel;
import dev.xhyrom.beamline.BeamlineMod;
import dev.xhyrom.beamline.network.BeamlinePacket;
import dev.xhyrom.beamline.network.SmartDisplayLinkConfigurationPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public enum Packets {
    CONFIGURE_SMART_DATA_GATHERER(SmartDisplayLinkConfigurationPacket.class, SmartDisplayLinkConfigurationPacket::new);

    public static final ResourceLocation CHANNEL_NAME = BeamlineMod.asResource("main");
    private static final NetworkChannel CHANNEL = NetworkChannel.create(CHANNEL_NAME);

    private final PacketType<?> packetType;

    <T extends BeamlinePacket> Packets(Class<T> type, Function<FriendlyByteBuf, T> factory) {
        packetType = new PacketType<>(type, factory);
    }

    public static void register() {
        for (Packets packet : values())
            packet.packetType.register();
    }

    public static void sendToServer(BeamlinePacket message) {
        CHANNEL.sendToServer(message);
    }

    private record PacketType<T extends BeamlinePacket>(Class<T> type, Function<FriendlyByteBuf, T> decoder) {
        private void register() {
                CHANNEL.register(
                        type,
                        T::write,
                        decoder,
                        T::handle
                );
            }
        }
}
