package dev.xhyrom.beamline.block.entity;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.api.contraption.transformable.TransformableBlockEntity;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelSupportBehaviour;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.LinkWithBulbBlockEntity;
import com.simibubi.create.foundation.advancement.AllAdvancements;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.xhyrom.beamline.block.SmartDisplayLinkBlock;
import dev.xhyrom.beamline.util.SmartDisplayLinkContext;
import lombok.Getter;
import lombok.Setter;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SmartDisplayLinkBlockEntity extends LinkWithBulbBlockEntity implements TransformableBlockEntity {
    public List<LinkConnection> connections = new ArrayList<>();

    public FactoryPanelSupportBehaviour factoryPanelSupport;

    public SmartDisplayLinkBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        behaviours.add(factoryPanelSupport = new FactoryPanelSupportBehaviour(this, () -> false, () -> false, () -> {
            for (LinkConnection conn : connections) {
                updateGatheredData(conn);
            }
        }));

        registerAwardables(behaviours, AllAdvancements.DISPLAY_LINK, AllAdvancements.DISPLAY_BOARD);
    }

    @Override
    public void tick() {
        super.tick();

        if (isVirtual())
            return;

        if (level.isClientSide)
            return;

        for (LinkConnection conn : connections) {
            if (conn.source == null)
                continue;

            conn.refreshTicks++;
            if (conn.refreshTicks < conn.source.getPassiveRefreshTicks() || !conn.source.shouldPassiveReset())
                continue;

            tickSource(conn);
        }
    }

    public void tickSources() {
        for (LinkConnection conn : connections) {
            tickSource(conn);
        }
    }

    private void tickSource(LinkConnection conn) {
        conn.refreshTicks = 0;
        if (getBlockState().getOptionalValue(SmartDisplayLinkBlock.POWERED)
                .orElse(true))
            return;

        if (!level.isClientSide)
            updateGatheredData(conn);
    }

    public void onNoLongerPowered() {
        for (LinkConnection conn : connections) {
            if (conn.source() == null)
                return;

            conn.refreshTicks(0);
            conn.source.onSignalReset(new SmartDisplayLinkContext(level, this, conn));
            updateGatheredData(conn);
        }
    }

    public void updateGatheredData(LinkConnection connection) {
        BlockPos sourcePosition = sourcePosition();
        BlockPos targetPosition = targetPosition(connection);

        if (!level.isLoaded(targetPosition) || !level.isLoaded(sourcePosition))
            return;

        DisplayTarget target = DisplayTarget.get(level, targetPosition);
        List<DisplaySource> sources = DisplaySource.getAll(level, sourcePosition);
        boolean notify = false;

        if (connection.target != target) {
            connection.target = target;
            notify = true;
        }

        if (connection.source != null && !sources.contains(connection.source)) {
            connection.source = null;
            connection.sourceConfig = new CompoundTag();
            notify = true;
        }

        if (notify)
            notifyUpdate();

        if (connection.source == null || connection.target == null)
            return;

        DisplayLinkContext context = new SmartDisplayLinkContext(level, this, connection);
        connection.source.transferData(context, connection.target, connection.targetLine);
        sendPulseNextSync();
        sendData();

        award(AllAdvancements.DISPLAY_LINK);
    }

    public Direction direction() {
        return getBlockState().getOptionalValue(SmartDisplayLinkBlock.FACING)
                .orElse(Direction.UP)
                .getOpposite();
    }

    public BlockPos sourcePosition() {
        for (FactoryPanelPosition position : factoryPanelSupport.getLinkedPanels())
            return position.pos();

        return worldPosition.relative(direction());
    }


    public BlockPos targetPosition(final LinkConnection connection) {
        return worldPosition.offset(connection.targetOffset);
    }

    @Override
    public void writeSafe(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeSafe(tag, registries);
        writeGatheredData(tag, false);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        writeGatheredData(tag, clientPacket);
    }

    private void writeGatheredData(CompoundTag tag, boolean clientPacket) {
        ListTag list = new ListTag();

        for (LinkConnection conn : connections) {
            CompoundTag connectionTag = new CompoundTag();

            connectionTag.put("TargetOffset", NbtUtils.writeBlockPos(conn.targetOffset));
            connectionTag.putInt("TargetLine", conn.targetLine);

            if (clientPacket && conn.target != null) {
                ResourceLocation id = CreateBuiltInRegistries.DISPLAY_TARGET.getKey(conn.target);
                if (id != null) {
                    connectionTag.putString("TargetType", id.toString());
                }
            }

            if (conn.source != null) {
                CompoundTag sourceData = conn.sourceConfig.copy();
                ResourceLocation id = CreateBuiltInRegistries.DISPLAY_SOURCE.getKey(conn.source);
                if (id != null) {
                    sourceData.putString("Id", id.toString());
                }
                connectionTag.put("Source", sourceData);
            }

            if (conn.name != null) {
                connectionTag.putString("Name", conn.name);
            }

            list.add(connectionTag);
        }

        tag.put("LinkedSources", list);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);

        connections.clear();

        if (tag.contains("LinkedSources")) {
            ListTag list = tag.getList("LinkedSources", Tag.TAG_COMPOUND);
            for (Tag t : list) {
                if (t instanceof CompoundTag connectionTag) {
                    BlockPos offset = NbtUtils.readBlockPos(connectionTag, "TargetOffset").orElse(BlockPos.ZERO);

                    DisplayTarget target = null;
                    if (clientPacket && connectionTag.contains("TargetType")) {
                        target = DisplayTarget.get(ResourceLocation.tryParse(connectionTag.getString("TargetType")));
                    }

                    DisplaySource source = null;
                    CompoundTag sourceConfig = new CompoundTag();

                    if (connectionTag.contains("Source")) {
                        CompoundTag sourceTag = connectionTag.getCompound("Source");
                        source = DisplaySource.get(ResourceLocation.tryParse(sourceTag.getString("Id")));

                        if (source != null)
                            sourceConfig = sourceTag.copy();
                    }

                    connections.add(new LinkConnection(
                            connectionTag.contains("Name") ? connectionTag.getString("Name") : null,
                            offset,
                            target,
                            source,
                            sourceConfig,
                            connectionTag.getInt("TargetLine")
                    ));
                }
            }
        }
    }

    private static final Vec3 bulbOffset = VecHelper.voxelSpace(11, 7, 5);
    private static final Vec3 bulbOffsetVertical = VecHelper.voxelSpace(5, 7, 11);
    @Override
    public Vec3 getBulbOffset(BlockState state) {
        if (state.getOptionalValue(SmartDisplayLinkBlock.FACING).orElse(Direction.UP).getAxis().isVertical())
            return bulbOffsetVertical;

        return bulbOffset;
    }

    @Override
    public Direction getBulbFacing(BlockState state) {
        return state.getValue(SmartDisplayLinkBlock.FACING);
    }

    @Override
    public void transform(BlockEntity blockEntity, StructureTransform transform) {
        for (LinkConnection conn : connections) {
            conn.targetOffset(transform.applyWithoutOffset(conn.targetOffset));
        }

        notifyUpdate();
    }

    @Setter
    @Getter
    public static class LinkConnection {
        private @Nullable String name;
        private @NotNull BlockPos targetOffset;
        private @Nullable DisplayTarget target;
        private @Nullable DisplaySource source;
        private @NotNull CompoundTag sourceConfig;
        private int targetLine;
        private int refreshTicks;

        public LinkConnection(@Nullable String name, @NotNull BlockPos targetOffset, @Nullable DisplayTarget target, @Nullable DisplaySource source, @NotNull CompoundTag sourceConfig, int targetLine) {
            this.name = name;
            this.targetOffset = targetOffset;
            this.target = target;
            this.source = source;
            this.sourceConfig = sourceConfig;
            this.targetLine = targetLine;
            this.refreshTicks = 0;
        }
    }
}
