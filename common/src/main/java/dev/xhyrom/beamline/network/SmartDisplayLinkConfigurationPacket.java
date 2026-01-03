package dev.xhyrom.beamline.network;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.foundation.blockEntity.SyncedBlockEntity;
import com.simibubi.create.foundation.utility.AdventureUtil;
import dev.architectury.networking.NetworkManager;
import dev.xhyrom.beamline.block.entity.SmartDisplayLinkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;
import java.util.function.Supplier;

public class SmartDisplayLinkConfigurationPacket implements BeamlinePacket {
    private BlockPos pos;
    private Optional<String> customName;
    private int linkIndex;
    private CompoundTag configData;
    private int targetLine;

    public SmartDisplayLinkConfigurationPacket(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        this.customName = buffer.readOptional(FriendlyByteBuf::readUtf);
        this.linkIndex = buffer.readVarInt();
        this.configData = buffer.readNbt();
        this.targetLine = buffer.readVarInt();
    }

    public SmartDisplayLinkConfigurationPacket(BlockPos pos, Optional<String> customName, int linkIndex, CompoundTag configData, int targetLine) {
        this.pos = pos;
        this.customName = customName;
        this.linkIndex = linkIndex;
        this.configData = configData;
        this.targetLine = targetLine;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeOptional(customName, FriendlyByteBuf::writeUtf);
        buf.writeVarInt(linkIndex);
        buf.writeNbt(configData);
        buf.writeVarInt(targetLine);
    }

    protected void applySettings(SmartDisplayLinkBlockEntity be) {
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
    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();

        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null || player.isSpectator() || AdventureUtil.isAdventure(player))
                return;
            Level world = player.level();
            if (!world.isLoaded(pos))
                return;
            if (!pos.closerThan(player.blockPosition(), maxRange()))
                return;
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof SyncedBlockEntity) {
                applySettings((SmartDisplayLinkBlockEntity) blockEntity);
                if (!causeUpdate())
                    return;
                ((SyncedBlockEntity) blockEntity).sendData();
                blockEntity.setChanged();
            }
        });
    }

    protected int maxRange() {
        return 20;
    }

    protected boolean causeUpdate() {
        return true;
    }
}