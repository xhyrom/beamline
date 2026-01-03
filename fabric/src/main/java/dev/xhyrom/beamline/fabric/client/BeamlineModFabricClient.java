package dev.xhyrom.beamline.fabric.client;

import dev.xhyrom.beamline.event.ClientEvents;
import net.fabricmc.api.ClientModInitializer;

public class BeamlineModFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientEvents.register();
    }
}
