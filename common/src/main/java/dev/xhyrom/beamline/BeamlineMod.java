package dev.xhyrom.beamline;

import com.simibubi.create.foundation.data.CreateRegistrate;
import dev.xhyrom.beamline.registry.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class BeamlineMod {
    public static final String ID = "create_beamline";
    public static final Logger LOGGER = LogManager.getLogger(ID);

    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(ID);

    static {
        REGISTRATE.defaultCreativeTab(ID, builder -> builder
                .title(Component.translatable("itemGroup.create_beamline"))
                .icon(Blocks.SMART_DISPLAY_LINK::asStack))
                .build();
    }

    public static void init() {
        Blocks.init();
        BlockEntities.init();
        Packets.register();
    }

    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(ID, path);
    }
}
