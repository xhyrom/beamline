package dev.xhyrom.beamline.item;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.xhyrom.beamline.util.BeamlineLang;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class MultipleClickToLinkBlockItem extends BlockItem implements BlockUseBypassingItem {
    public MultipleClickToLinkBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean shouldBypass(BlockState state, BlockPos pos, Level level, Player player, InteractionHand hand) {
        ItemStack usedItem = player.getItemInHand(hand);
        return usedItem.getItem() instanceof MultipleClickToLinkBlockItem;
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext pContext) {
        ItemStack stack = pContext.getItemInHand();
        BlockPos clickedPos = pContext.getClickedPos();
        Level level = pContext.getLevel();
        Player player = pContext.getPlayer();
        String msgKey = getMessageTranslationKey();

        if (player == null)
            return InteractionResult.FAIL;

        if (player.isShiftKeyDown() && hasSelections(stack)) {
            if (level.isClientSide)
                return InteractionResult.SUCCESS;

            player.displayClientMessage(BeamlineLang.translateDirect(msgKey + ".clear"), true);
            clearSelections(stack);
            return InteractionResult.SUCCESS;
        }

        BlockState clickedState = level.getBlockState(clickedPos);

        if (clickedState.is(this.getBlock())) {
            if (!hasSelections(stack)) {
                player.displayClientMessage(BeamlineLang.translateDirect(msgKey + ".no_selections").withStyle(ChatFormatting.RED), true);
                return InteractionResult.PASS;
            }

            if (level.isClientSide) return InteractionResult.SUCCESS;

            BlockEntity be = level.getBlockEntity(clickedPos);
            int added = addLinksToExisting(be, selections(stack), level);

            if (added > 0) {
                if (be instanceof SmartBlockEntity sbe) {
                    sbe.notifyUpdate();
                }

                player.displayClientMessage(BeamlineLang.translateDirect(msgKey + ".added_sources", added).withStyle(ChatFormatting.GREEN), true);
                clearSelections(stack);
                return InteractionResult.SUCCESS;
            } else {
                player.displayClientMessage(BeamlineLang.translateDirect(msgKey + ".no_new_sources").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
        }

        ResourceLocation currentDim = level.dimension().location();

        if (isValidTarget(level, clickedPos)) {
            if (level.isClientSide) return InteractionResult.SUCCESS;

            if (isPositionSelected(stack, clickedPos, level)) {
                player.displayClientMessage(BeamlineLang.translate(msgKey + ".already_selected").style(ChatFormatting.RED).component(), true);
                return InteractionResult.FAIL;
            }

            if (hasSelections(stack) && !Objects.equals(getLastDimension(stack), currentDim)) {
                player.displayClientMessage(BeamlineLang.translate(msgKey + ".dimension_mismatch").style(ChatFormatting.RED).component(), true);
                return InteractionResult.FAIL;
            }

            addSelection(stack, clickedPos, currentDim);
            int count = getSelectionCount(stack);

            player.displayClientMessage(BeamlineLang.translateDirect(msgKey + ".set").append(" (#" + count + ")"), true);
            return InteractionResult.SUCCESS;
        }

        if (!hasSelections(stack)) {
            if (level.isClientSide) return InteractionResult.FAIL;

            player.displayClientMessage(BeamlineLang.translateDirect(msgKey + ".invalid"), true);
            return InteractionResult.FAIL;
        }

        BlockPos placedPos = clickedPos.relative(pContext.getClickedFace(), level.getBlockState(clickedPos).canBeReplaced() ? 0 : 1);

        int maxDist = getMaxDistanceFromSelection();
        List<LinkData> selections = selections(stack);

        for (LinkData sel : selections) {
            if (maxDist != -1 && !sel.pos.closerThan(placedPos, maxDist)) {
                player.displayClientMessage(BeamlineLang.translateDirect(msgKey + ".too_far").withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
        }

        CompoundTag beTag = new CompoundTag();
        ListTag linksList = new ListTag();

        for (LinkData sel : selections) {
            CompoundTag linkTag = new CompoundTag();
            linkTag.put("TargetOffset", NbtUtils.writeBlockPos(sel.pos.subtract(placedPos)));
            linkTag.putString("TargetDimension", sel.dim.toString());
            linksList.add(linkTag);
        }

        beTag.put("LinkedSources", linksList);

        CompoundTag stackTag = stack.getOrCreateTag();
        stackTag.put("BlockEntityTag", beTag);

        InteractionResult result = super.useOn(pContext);

        if (!level.isClientSide && result.consumesAction()) {
            clearSelections(stack);
            player.displayClientMessage(BeamlineLang.translateDirect(msgKey + ".success").withStyle(ChatFormatting.GREEN), true);
        }

        return result;
    }

    public record LinkData(BlockPos pos, ResourceLocation dim) {}

    protected void addSelection(ItemStack stack, BlockPos pos, ResourceLocation dim) {
        CompoundTag tag = stack.getOrCreateTag();
        ListTag list = tag.getList("SelectionList", Tag.TAG_COMPOUND);

        CompoundTag entry = new CompoundTag();
        entry.put("Pos", NbtUtils.writeBlockPos(pos));
        entry.putString("Dim", dim.toString());

        list.add(entry);
        tag.put("SelectionList", list);
    }

    public List<LinkData> selections(ItemStack stack) {
        List<LinkData> results = new ArrayList<>();
        if (!stack.hasTag()) return results;

        CompoundTag tag = stack.getTag();
        if (tag == null) return results;

        ListTag list = tag.getList("SelectionList", Tag.TAG_COMPOUND);

        for (Tag t : list) {
            if (t instanceof CompoundTag ct) {
                BlockPos p = NbtUtils.readBlockPos(ct.getCompound("Pos"));
                ResourceLocation d = new ResourceLocation(ct.getString("Dim"));
                results.add(new LinkData(p, d));
            }
        }
        return results;
    }

    protected int addLinksToExisting(BlockEntity be, List<LinkData> selections, Level level) {
        return 0;
    }

    protected boolean isPositionSelected(ItemStack stack, BlockPos pos, Level level) {
        return selections(stack).stream().anyMatch(d -> d.pos.equals(pos));
    }

    protected void clearSelections(ItemStack stack) {
        if (stack.hasTag()) {
            CompoundTag tag = stack.getTag();
            tag.remove("SelectionList");
            tag.remove("BlockEntityTag");
        }
    }

    protected boolean hasSelections(ItemStack stack) {
        return getSelectionCount(stack) > 0;
    }

    protected int getSelectionCount(ItemStack stack) {
        if (!stack.hasTag()) return 0;
        return stack.getTag().getList("SelectionList", Tag.TAG_COMPOUND).size();
    }

    private ResourceLocation getLastDimension(ItemStack stack) {
        List<LinkData> sels = selections(stack);

        if (sels.isEmpty()) return null;
        return sels.get(sels.size() - 1).dim;
    }

    public abstract int getMaxDistanceFromSelection();

    public abstract String getMessageTranslationKey();

    public boolean isValidTarget(LevelAccessor level, BlockPos pos) {
        return true;
    }

    @Environment(EnvType.CLIENT)
    public AABB selectionBounds(BlockPos pos) {
        Level world = Minecraft.getInstance().level;
        BlockState state = world.getBlockState(pos);
        VoxelShape shape = state.getShape(world, pos);
        return shape.isEmpty() ? new AABB(BlockPos.ZERO)
                : shape.bounds()
                .move(pos);
    }
}