package dev.xhyrom.beamline.util;

import dev.xhyrom.beamline.BeamlineMod;
import net.createmod.catnip.lang.Lang;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class BeamlineLang extends Lang {
    public static MutableComponent translateDirect(String key, Object... args) {
        Object[] args1 = LangBuilder.resolveBuilders(args);
        return Component.translatable(BeamlineMod.ID + "." + key, args1);
    }

    public static LangBuilder builder() {
        return new LangBuilder(BeamlineMod.ID);
    }

    public static LangBuilder translate(String langKey, Object... args) {
        return builder().translate(langKey, args);
    }
}
