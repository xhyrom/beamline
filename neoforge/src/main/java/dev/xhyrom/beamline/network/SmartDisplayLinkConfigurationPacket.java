package dev.xhyrom.beamline.network;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.foundation.networking.BlockEntityConfigurationPacket;
import dev.xhyrom.beamline.block.entity.SmartDisplayLinkBlockEntity;
import dev.xhyrom.beamline.registry.Packets;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class SmartDisplayLinkConfigurationPacket extends BlockEntityConfigurationPacket<SmartDisplayLinkBlockEntity> {
    public static final StreamCodec<ByteBuf, SmartDisplayLinkConfigurationPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            packet -> packet.pos,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8),
            packet -> packet.customName,
            ByteBufCodecs.VAR_INT,
            packet -> packet.linkIndex,
            ByteBufCodecs.COMPOUND_TAG,
            packet -> packet.configData,
            ByteBufCodecs.VAR_INT,
            packet -> packet.targetLine,
            SmartDisplayLinkConfigurationPacket::new
    );

    private Optional<String> customName;
    private final int linkIndex;
    private final CompoundTag configData;
    private final int targetLine;

    public SmartDisplayLinkConfigurationPacket(BlockPos pos, int linkIndex, CompoundTag configData, int targetLine) {
        this(pos, Optional.empty(), linkIndex, configData, targetLine);
    }

    public SmartDisplayLinkConfigurationPacket(BlockPos pos, Optional<String> customName, int linkIndex, CompoundTag configData, int targetLine) {
        super(pos);
        this.customName = customName;
        this.linkIndex = linkIndex;
        this.configData = configData;
        this.targetLine = targetLine;
    }

    @Override
    protected void applySettings(ServerPlayer player, SmartDisplayLinkBlockEntity be) {
        if (!configData.contains("Id")) {
            be.notifyUpdate();
            return;
        }

        ResourceLocation id = ResourceLocation.tryParse(configData.getString("Id"));
        DisplaySource source = DisplaySource.get(id);

        if (source == null) {
            be.notifyUpdate();
            return;
        }

        final SmartDisplayLinkBlockEntity.LinkConnection connection = be.connections.get(linkIndex);

        connection.targetLine(targetLine);
        customName.ifPresent(connection::name);

        if (connection.source() == null || connection.source() != source) {
            connection.source(source);
            connection.sourceConfig(configData.copy());
        } else {
            connection.sourceConfig()
                    .merge(configData);
        }

        be.updateGatheredData(connection);
        be.notifyUpdate();
    }

    @Override
    public PacketTypeProvider getTypeProvider() {
        return Packets.CONFIGURE_SMART_DATA_GATHERER;
    }
}