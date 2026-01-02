package dev.xhyrom.beamline.neoforge;

import dev.xhyrom.beamline.BeamlineMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@Mod(BeamlineMod.ID)
public class BeamlineNeoForge {
    public BeamlineNeoForge(final ModContainer container) {
        container.getEventBus().register(this);

        BeamlineMod.REGISTRATE.registerEventListeners(container.getEventBus());
        BeamlineMod.init();
    }

    @SubscribeEvent
    public void onInit(FMLCommonSetupEvent event) {
        BeamlineMod.LOGGER.info("Hello from Forge!");
    }

    @SubscribeEvent
    public void register(final RegisterPayloadHandlersEvent event) {}
}
