package dev.xhyrom.beamline.util;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import dev.xhyrom.beamline.block.entity.SmartDisplayLinkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public class SmartDisplayLinkContext extends DisplayLinkContext {
    private final SmartDisplayLinkBlockEntity.LinkConnection connection;
    private final SmartDisplayLinkBlockEntity smartDisplayLinkBlockEntity;

    public static DisplayLinkBlockEntity createEntity(SmartDisplayLinkBlockEntity be, SmartDisplayLinkBlockEntity.LinkConnection connection) {
        final DisplayLinkBlockEntity proxy = new DisplayLinkBlockEntity(be.getType(), be.getBlockPos(), be.getBlockState());

        proxy.activeSource = connection.source();
        proxy.activeTarget = connection.target();
        proxy.targetLine = connection.targetLine();
        proxy.factoryPanelSupport = be.factoryPanelSupport;

        return proxy;
    }

    public SmartDisplayLinkContext(Level level, SmartDisplayLinkBlockEntity be, SmartDisplayLinkBlockEntity.LinkConnection connection) {
        super(level, createEntity(be, connection));

        this.smartDisplayLinkBlockEntity = be;
        this.connection = connection;
    }

    public BlockPos getTargetPos() {
        return smartDisplayLinkBlockEntity.targetPosition(connection);
    }

    public CompoundTag sourceConfig() {
        return connection.sourceConfig();
    }
}