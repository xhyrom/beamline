package dev.xhyrom.beamline.block;

import com.mojang.serialization.MapCodec;
import com.simibubi.create.AllShapes;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.source.RedstonePowerDisplaySource;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.block.WrenchableDirectionalBlock;
import dev.xhyrom.beamline.block.entity.SmartDisplayLinkBlockEntity;
import dev.xhyrom.beamline.client.gui.SmartDisplayLinkSelectionScreen;
import dev.xhyrom.beamline.registry.BlockEntities;
import dev.xhyrom.beamline.registry.Blocks;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SmartDisplayLinkBlock extends WrenchableDirectionalBlock implements IBE<SmartDisplayLinkBlockEntity> {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public static final MapCodec<SmartDisplayLinkBlock> CODEC = simpleCodec(SmartDisplayLinkBlock::new);

    public SmartDisplayLinkBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(POWERED, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState placed = super.getStateForPlacement(context);
        assert placed != null;

        placed = placed.setValue(FACING, context.getClickedFace());
        return placed.setValue(POWERED, shouldBePowered(placed, context.getLevel(), context.getClickedPos()));
    }

    @Override
    public void setPlacedBy(@NotNull Level pLevel, @NotNull BlockPos pPos, @NotNull BlockState pState, LivingEntity pPlacer, @NotNull ItemStack pStack) {
        super.setPlacedBy(pLevel, pPos, pState, pPlacer, pStack);
        AdvancementBehaviour.setPlacedBy(pLevel, pPos, pPlacer);
    }

    @Override
    public void onRemove(@NotNull BlockState pState, @NotNull Level pLevel, @NotNull BlockPos pPos, @NotNull BlockState pNewState, boolean pMovedByPiston) {
        IBE.onRemove(pState, pLevel, pPos, pNewState);
    }

    public static void notifyGatherers(LevelAccessor level, BlockPos pos) {
        forEachAttachedGatherer(level, pos, SmartDisplayLinkBlockEntity::tickSources);
    }

    @SuppressWarnings("unchecked")
    public static <T extends DisplaySource> void sendToGatherers(LevelAccessor level, BlockPos pos,
                                                                 BiConsumer<SmartDisplayLinkBlockEntity, T> callback, Class<T> type) {
        forEachAttachedGatherer(level, pos, dgte -> {
            for (SmartDisplayLinkBlockEntity.LinkConnection conn : dgte.connections) {
                if (conn.source() == null || !type.isInstance(conn.source()))
                    continue;
                callback.accept(dgte, (T) conn.source());
            }
        });
    }

    private static void forEachAttachedGatherer(LevelAccessor level, BlockPos pos,
                                                Consumer<SmartDisplayLinkBlockEntity> callback) {
        for (Direction d : Iterate.directions) {
            BlockPos offsetPos = pos.relative(d);
            BlockState blockState = level.getBlockState(offsetPos);
            if (!Blocks.SMART_DISPLAY_LINK.has(blockState))
                continue;

            BlockEntity blockEntity = level.getBlockEntity(offsetPos);
            if (!(blockEntity instanceof SmartDisplayLinkBlockEntity dlbe))
                continue;
            if (dlbe.direction() != d.getOpposite())
                continue;

            callback.accept(dlbe);
        }
    }

    @Override
    public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos,
                                boolean isMoving) {
        if (worldIn.isClientSide)
            return;

        if (fromPos.equals(pos.relative(state.getValue(FACING)
                .getOpposite())))
            sendToGatherers(worldIn, fromPos, (dlte, p) -> dlte.tickSources(), RedstonePowerDisplaySource.class);

        boolean powered = shouldBePowered(state, worldIn, pos);
        boolean previouslyPowered = state.getValue(POWERED);
        if (previouslyPowered != powered) {
            worldIn.setBlock(pos, state.cycle(POWERED), Block.UPDATE_CLIENTS);
            if (!powered)
                withBlockEntityDo(worldIn, pos, SmartDisplayLinkBlockEntity::onNoLongerPowered);
        }
    }

    private boolean shouldBePowered(BlockState state, Level worldIn, BlockPos pos) {
        boolean powered = false;
        for (Direction d : Iterate.directions) {
            if (d.getOpposite() == state.getValue(FACING))
                continue;
            if (worldIn.getSignal(pos.relative(d), d) == 0)
                continue;
            powered = true;
            break;
        }
        return powered;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder.add(POWERED));
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, Player player, @NotNull BlockHitResult hitResult) {
        if (player.isShiftKeyDown())
            return InteractionResult.PASS;

        CatnipServices.PLATFORM.executeOnClientOnly(() -> () -> withBlockEntityDo(level, pos, be -> this.displayScreen(be, player)));
        return InteractionResult.SUCCESS;
    }

    @OnlyIn(value = Dist.CLIENT)
    protected void displayScreen(SmartDisplayLinkBlockEntity be, Player player) {
        if (!(player instanceof LocalPlayer))
            return;

        ScreenOpener.open(new SmartDisplayLinkSelectionScreen(be));
    }

    @Override
    protected boolean isPathfindable(@NotNull BlockState state, @NotNull PathComputationType pathComputationType) {
        return false;
    }

    @Override
    public @NotNull VoxelShape getShape(BlockState pState, @NotNull BlockGetter pLevel, @NotNull BlockPos pPos, @NotNull CollisionContext pContext) {
        return AllShapes.DATA_GATHERER.get(pState.getValue(FACING));
    }

    @Override
    public Class<SmartDisplayLinkBlockEntity> getBlockEntityClass() {
        return SmartDisplayLinkBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SmartDisplayLinkBlockEntity> getBlockEntityType() {
        return BlockEntities.SMART_DISPLAY_LINK.get();
    }

    @Override
    protected @NotNull MapCodec<? extends DirectionalBlock> codec() {
        return CODEC;
    }
}
