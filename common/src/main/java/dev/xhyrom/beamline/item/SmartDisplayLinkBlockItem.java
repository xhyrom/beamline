package dev.xhyrom.beamline.item;

import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.infrastructure.config.AllConfigs;
import dev.xhyrom.beamline.block.entity.SmartDisplayLinkBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

import java.util.List;

public class SmartDisplayLinkBlockItem extends MultipleClickToLinkBlockItem {
    public SmartDisplayLinkBlockItem(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }

    @Environment(EnvType.CLIENT)
    public AABB selectionBounds(BlockPos pos) {
        final Level world = Minecraft.getInstance().level;
        assert world != null;

        final DisplayTarget target = DisplayTarget.get(world, pos);
        if (target != null)
            return target.getMultiblockBounds(world, pos);

        return super.selectionBounds(pos);
    }

    @Override
    public int getMaxDistanceFromSelection() {
        return AllConfigs.server().logistics.displayLinkRange.get();
    }

    @Override
    public String getMessageTranslationKey() {
        return "smart_display_link";
    }

    protected boolean arePositionsEquivalent(BlockPos posA, BlockPos posB, Level level) {
        if (posA.equals(posB)) return true;

        DisplayTarget targetA = DisplayTarget.get(level, posA);
        DisplayTarget targetB = DisplayTarget.get(level, posB);

        if (targetA == null || targetB == null) return false;

        AABB boundsA = targetA.getMultiblockBounds(level, posA);
        AABB boundsB = targetB.getMultiblockBounds(level, posB);

        return boundsA.intersects(boundsB);
    }

    @Override
    protected boolean isPositionSelected(ItemStack stack, BlockPos pos, Level level) {
        return selections(stack).stream()
                .anyMatch(data -> arePositionsEquivalent(pos, data.pos(), level));
    }

    @Override
    protected int addLinksToExisting(BlockEntity be, List<LinkData> selections, Level level) {
        if (!(be instanceof SmartDisplayLinkBlockEntity linkBe)) return 0;

        int count = 0;
        BlockPos bePos = linkBe.getBlockPos();

        for (LinkData sel : selections) {
            BlockPos newTargetPos = sel.pos();

            boolean alreadyExists = linkBe.connections.stream().anyMatch(conn -> {
                BlockPos existingTargetPos = bePos.offset(conn.targetOffset());
                return arePositionsEquivalent(newTargetPos, existingTargetPos, level);
            });

            if (alreadyExists)
                continue;

            linkBe.connections.add(new SmartDisplayLinkBlockEntity.LinkConnection(
                    null,
                    newTargetPos.subtract(bePos),
                    null,
                    null,
                    new CompoundTag(),
                    0
            ));
            count++;
        }

        return count;
    }

    @Override
    public boolean isValidTarget(LevelAccessor level, BlockPos pos) {
        return DisplayTarget.get(level, pos) != null;
    }
}
