package dev.xhyrom.beamline.event;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.xhyrom.beamline.item.MultipleClickToLinkBlockItem;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.awt.*;
import java.util.List;

public class ClientEvents {
    public static void register() {
        ClientTickEvent.CLIENT_POST.register(ClientEvents::onClientTick);
    }

    private static void onClientTick(Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null) return;

        ItemStack heldItem = player.getMainHandItem();
        if (!(heldItem.getItem() instanceof MultipleClickToLinkBlockItem blockItem)) return;

        List<MultipleClickToLinkBlockItem.LinkData> selections = blockItem.selections(heldItem);
        if (selections.isEmpty()) return;

        Level world = minecraft.level;
        if (world == null) return;

        int index = 0;

        for (MultipleClickToLinkBlockItem.LinkData sel : selections) {
            if (!sel.dim().equals(world.dimension().location())) {
                index++;
                continue;
            }

            AABB bounds = blockItem.selectionBounds(sel.pos());

            float hue = (0.08f + (index * 0.1f)) % 1.0f;
            int color = Color.HSBtoRGB(hue, 1.0f, 1.0f);

            Outliner.getInstance().showAABB("target_" + index, bounds)
                    .colored(color)
                    .lineWidth(1 / 16f);

            index++;
        }
    }
}
