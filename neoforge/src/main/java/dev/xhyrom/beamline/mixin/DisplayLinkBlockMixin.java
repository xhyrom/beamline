package dev.xhyrom.beamline.mixin;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlock;
import dev.xhyrom.beamline.block.SmartDisplayLinkBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisplayLinkBlock.class)
public class DisplayLinkBlockMixin {
    @Inject(method = "notifyGatherers", at = @At("HEAD"))
    private static void beamline$notifyGatherersInject(LevelAccessor level, BlockPos pos, CallbackInfo ci) {
        SmartDisplayLinkBlock.notifyGatherers(level, pos);
    }
}
