package dev.xhyrom.beamline.registry;

import com.simibubi.create.content.redstone.displayLink.LinkBulbRenderer;
import com.tterrag.registrate.util.entry.BlockEntityEntry;
import dev.xhyrom.beamline.BeamlineMod;
import dev.xhyrom.beamline.block.entity.SmartDisplayLinkBlockEntity;

public class BlockEntities {
    public static final BlockEntityEntry<SmartDisplayLinkBlockEntity> SMART_DISPLAY_LINK = BeamlineMod.REGISTRATE
            .blockEntity("smart_display_link", SmartDisplayLinkBlockEntity::new)
            .validBlocks(Blocks.SMART_DISPLAY_LINK)
            .renderer(() -> LinkBulbRenderer::new)
            .register();


    public static void init() {}
}
