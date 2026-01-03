package dev.xhyrom.beamline.fabric;

import dev.xhyrom.beamline.BeamlineMod;
import net.fabricmc.api.ModInitializer;

public class BeamlineModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        BeamlineMod.init();
        BeamlineMod.REGISTRATE.register();
    }
}
