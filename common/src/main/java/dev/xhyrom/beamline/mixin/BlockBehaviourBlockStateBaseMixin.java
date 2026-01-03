// https://github.com/Fabricators-of-Create/Porting-Lib/blob/1.21.1/modules/items/src/main/java/io/github/fabricators_of_create/porting_lib/item/mixin/common/BlockBehaviour%24BlockStateBaseMixin.java#L29

package dev.xhyrom.beamline.mixin;

import dev.xhyrom.beamline.item.BlockUseBypassingItem;
import net.minecraft.world.InteractionResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.BlockHitResult;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockBehaviourBlockStateBaseMixin {
    @Inject(at = @At("HEAD"), method = "use", cancellable = true)
    private void use(Level level, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult, CallbackInfoReturnable<InteractionResult> cir) {
        Item held = player.getItemInHand(interactionHand).getItem();
        BlockPos pos = blockHitResult.getBlockPos();
        if (held instanceof BlockUseBypassingItem bypassing) {
            if (bypassing.shouldBypass(level.getBlockState(pos), pos, level, player, interactionHand))
                cir.setReturnValue(InteractionResult.PASS);
        } else if (held instanceof BlockItem blockItem && blockItem.getBlock() instanceof BlockUseBypassingItem bypassing) {
            if (bypassing.shouldBypass(level.getBlockState(pos), pos, level, player, interactionHand))
                cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
