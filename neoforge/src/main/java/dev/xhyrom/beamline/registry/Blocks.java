package dev.xhyrom.beamline.registry;

import com.simibubi.create.foundation.data.AssetLookup;
import com.simibubi.create.foundation.data.SharedProperties;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.xhyrom.beamline.BeamlineMod;
import dev.xhyrom.beamline.block.SmartDisplayLinkBlock;
import dev.xhyrom.beamline.item.SmartDisplayLinkBlockItem;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.material.MapColor;

import static com.simibubi.create.foundation.data.ModelGen.customItemModel;
import static com.simibubi.create.foundation.data.TagGen.axeOrPickaxe;

public final class Blocks {
    public static final BlockEntry<SmartDisplayLinkBlock> SMART_DISPLAY_LINK =
            BeamlineMod.REGISTRATE.block("smart_display_link", SmartDisplayLinkBlock::new)
                    .initialProperties(SharedProperties::softMetal)
                    .properties(p -> p.mapColor(MapColor.TERRACOTTA_BROWN))
                    .addLayer(() -> RenderType::translucent)
                    .transform(axeOrPickaxe())
                    .blockstate((c, p) -> p.directionalBlock(c.get(), AssetLookup.forPowered(c, p)))
                    .item(SmartDisplayLinkBlockItem::new)
                    .transform(customItemModel("_", "block"))
                    .register();

    public static void init() {}
}
