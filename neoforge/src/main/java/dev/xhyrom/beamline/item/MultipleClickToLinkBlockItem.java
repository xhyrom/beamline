package dev.xhyrom.beamline.item;

import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import dev.xhyrom.beamline.util.BeamlineLang;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@EventBusSubscriber
public abstract class MultipleClickToLinkBlockItem extends BlockItem {
    public MultipleClickToLinkBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @SubscribeEvent
    public static void linkableItemAlwaysPlacesWhenUsed(PlayerInteractEvent.RightClickBlock event) {
        ItemStack usedItem = event.getItemStack();
        if (!(usedItem.getItem() instanceof MultipleClickToLinkBlockItem))
            return;

        event.setUseBlock(TriState.FALSE);
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
            int added = addLinksToExisting(be, getSelections(stack), level);

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
        List<LinkData> selections = getSelections(stack);

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

        BlockEntity.addEntityType(beTag, ((IBE<?>) this.getBlock()).getBlockEntityType());
        stack.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(beTag));

        InteractionResult result = super.useOn(pContext);

        if (!level.isClientSide && result.consumesAction()) {
            clearSelections(stack);
            player.displayClientMessage(BeamlineLang.translateDirect(msgKey + ".success").withStyle(ChatFormatting.GREEN), true);
        }

        return result;
    }

    public record LinkData(BlockPos pos, ResourceLocation dim) {}

    protected void addSelection(ItemStack stack, BlockPos pos, ResourceLocation dim) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = data.copyTag();
        ListTag list = tag.getList("SelectionList", Tag.TAG_COMPOUND);

        CompoundTag entry = new CompoundTag();
        entry.put("Pos", NbtUtils.writeBlockPos(pos));
        entry.putString("Dim", dim.toString());

        list.add(entry);
        tag.put("SelectionList", list);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    protected List<LinkData> getSelections(ItemStack stack) {
        List<LinkData> results = new ArrayList<>();
        if (!stack.has(DataComponents.CUSTOM_DATA)) return results;

        CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        ListTag list = tag.getList("SelectionList", Tag.TAG_COMPOUND);

        for (Tag t : list) {
            if (t instanceof CompoundTag ct) {
                BlockPos p = NbtUtils.readBlockPos(ct, "Pos").orElse(BlockPos.ZERO);
                ResourceLocation d = ResourceLocation.tryParse(ct.getString("Dim"));
                results.add(new LinkData(p, d));
            }
        }
        return results;
    }

    protected int addLinksToExisting(BlockEntity be, List<LinkData> selections, Level level) {
        return 0;
    }

    protected boolean isPositionSelected(ItemStack stack, BlockPos pos, Level level) {
        return getSelections(stack).stream().anyMatch(d -> d.pos.equals(pos));
    }

    protected void clearSelections(ItemStack stack) {
        stack.remove(DataComponents.CUSTOM_DATA);
        stack.remove(DataComponents.BLOCK_ENTITY_DATA);
    }

    protected boolean hasSelections(ItemStack stack) {
        return getSelectionCount(stack) > 0;
    }

    protected int getSelectionCount(ItemStack stack) {
        if (!stack.has(DataComponents.CUSTOM_DATA)) return 0;
        return stack.get(DataComponents.CUSTOM_DATA).copyTag().getList("SelectionList", Tag.TAG_COMPOUND).size();
    }

    private ResourceLocation getLastDimension(ItemStack stack) {
        List<LinkData> sels = getSelections(stack);

        if (sels.isEmpty()) return null;
        return sels.getLast().dim;
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void clientTick(ClientTickEvent.Post event) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof MultipleClickToLinkBlockItem blockItem)) return;

        List<LinkData> selections = blockItem.getSelections(heldItem);
        if (selections.isEmpty()) return;

        Level world = Minecraft.getInstance().level;
        if (world == null) return;

        int index = 0;

        for (LinkData sel : selections) {
            if (!sel.dim.equals(world.dimension().location())) {
                index++;
                continue;
            }

            AABB bounds = blockItem.getSelectionBounds(sel.pos);

            float hue = (0.08f + (index * 0.1f)) % 1.0f;
            int color = Color.HSBtoRGB(hue, 1.0f, 1.0f);

            Outliner.getInstance().showAABB("target_" + index, bounds)
                    .colored(color)
                    .lineWidth(1 / 16f);

            index++;
        }
    }

    public abstract int getMaxDistanceFromSelection();

    public abstract String getMessageTranslationKey();

    public boolean isValidTarget(LevelAccessor level, BlockPos pos) {
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public AABB getSelectionBounds(BlockPos pos) {
        Level world = Minecraft.getInstance().level;
        BlockState state = world.getBlockState(pos);
        VoxelShape shape = state.getShape(world, pos);
        return shape.isEmpty() ? new AABB(BlockPos.ZERO)
                : shape.bounds()
                .move(pos);
    }
}