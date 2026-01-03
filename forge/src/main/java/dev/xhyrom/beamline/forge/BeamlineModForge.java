package dev.xhyrom.beamline.forge;

import dev.architectury.platform.forge.EventBuses;
import dev.xhyrom.beamline.BeamlineMod;
import dev.xhyrom.beamline.event.ClientEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BeamlineMod.ID)
public class BeamlineModForge {
    public BeamlineModForge() {
        EventBuses.registerModEventBus(BeamlineMod.ID, FMLJavaModLoadingContext.get().getModEventBus());
        BeamlineMod.REGISTRATE.registerEventListeners(FMLJavaModLoadingContext.get().getModEventBus());
        BeamlineMod.init();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientEvents::register);
    }
}
