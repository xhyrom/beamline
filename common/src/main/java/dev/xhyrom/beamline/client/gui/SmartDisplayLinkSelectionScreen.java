package dev.xhyrom.beamline.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import dev.xhyrom.beamline.block.entity.SmartDisplayLinkBlockEntity;
import dev.xhyrom.beamline.registry.GuiTextures;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.animation.LerpedFloat.Chaser;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SmartDisplayLinkSelectionScreen extends AbstractSimiScreen {

    private static final int CARD_HEADER = 28;
    private static final int CARD_WIDTH = 195;

    private final SmartDisplayLinkBlockEntity be;
    private final GuiTextures background;

    private final LerpedFloat scroll = LerpedFloat.linear().startWithValue(0);

    public SmartDisplayLinkSelectionScreen(SmartDisplayLinkBlockEntity be) {
        this.be = be;
        this.background = GuiTextures.DISPLAY_LINK_SELECTION;
    }

    @Override
    protected void init() {
        setWindowSize(background.getWidth(), background.getHeight());
        super.init();
        clearWidgets();

        final IconButton confirmButton = new IconButton(guiLeft + background.getWidth() - 42, guiTop + background.getHeight() - 30, AllIcons.I_CONFIRM);
        confirmButton.withCallback(this::onClose);
        addRenderableWidget(confirmButton);
    }

    @Override
    public void tick() {
        super.tick();
        scroll.tickChaser();
    }

    @Override
    protected void renderWindow(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x = guiLeft;
        int y = guiTop;

        background.render(graphics, x, y);

        FormattedCharSequence title = Component.translatable("create_beamline.screen.smart_display_link_selection.title").getVisualOrderText();
        int center = x + (background.getWidth() - 8) / 2;
        graphics.drawString(font, title,center - font.width(title) / 2, y + 4, 0x505050, false);

        if (be.connections.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable("create_beamline.screen.smart_display_link_selection.no_links"), center, y + 80, 0x808080);
        }

        renderCards(graphics, mouseX, mouseY, partialTicks);

        int zLevel = 200;
        graphics.fillGradient(x + 16, y + 16, x + 16 + 220, y + 16 + 10, zLevel, 0x77000000, 0x00000000);
        graphics.fillGradient(x + 16, y + 179, x + 16 + 220, y + 179 + 10, zLevel, 0x00000000, 0x77000000);
    }

    private void renderCards(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        PoseStack ms = graphics.pose();

        UIRenderHelper.drawStretched(graphics, guiLeft + 33, guiTop + 16, 3, 173, 200, AllGuiTextures.SCHEDULE_STRIP_DARK);

        float scrollOffset = -scroll.getValue(partialTicks);
        int yOffset = 0;

        graphics.enableScissor(guiLeft + 16, guiTop + 16, guiLeft + 236, guiTop + 189);

        ms.pushPose();
        ms.translate(0, scrollOffset, 0);

        List<SmartDisplayLinkBlockEntity.LinkConnection> connections = be.connections;

        for (int i = 0; i < connections.size(); i++) {
            SmartDisplayLinkBlockEntity.LinkConnection conn = connections.get(i);

            renderCard(graphics, conn, i, guiLeft + 25, guiTop + 25 + yOffset, mouseX, mouseY);

            yOffset += CARD_HEADER + 4;
        }

        ms.popPose();
        graphics.disableScissor();
    }

    private void renderCard(GuiGraphics graphics, SmartDisplayLinkBlockEntity.LinkConnection conn, int index, int x, int y, int mouseX, int mouseY) {
        int width = CARD_WIDTH;
        int height = CARD_HEADER;

        AllGuiTextures light = AllGuiTextures.SCHEDULE_CARD_LIGHT;
        AllGuiTextures medium = AllGuiTextures.SCHEDULE_CARD_MEDIUM;

        UIRenderHelper.drawStretched(graphics, x, y, width, height, 0, light);
        UIRenderHelper.drawStretched(graphics, x + 1, y + 1, width - 2, height - 2, 0, medium);

        UIRenderHelper.drawStretched(graphics, x + 8, y, 3, height, 0, AllGuiTextures.SCHEDULE_STRIP_LIGHT);

        ClientLevel level = minecraft.level;
        BlockPos sourcePos = be.getBlockPos().offset(conn.targetOffset());
        BlockState state = level.getBlockState(sourcePos);
        ItemStack iconStack = new ItemStack(state.getBlock().asItem());
        if (iconStack.isEmpty()) iconStack = new ItemStack(Items.BARRIER);

        GuiGameElement.of(iconStack).at(x + 14, y + 5).render(graphics);

        String displayName;
        if (conn.name() != null && !conn.name().isEmpty()) {
            displayName = conn.name();
        } else {
            displayName = (index + 1) + ". " + state.getBlock().getName().getString();
        }
        graphics.drawString(font, displayName, x + 36, y + 10, 0xFFFFFF, false);

        int buttonX = x + width - 24;
        int buttonY = y + 4;
        double scrolledY = buttonY - scroll.getValue(0);

        AllIcons icon = AllIcons.I_CONFIG_OPEN;
        PoseStack ms = graphics.pose();
        ms.pushPose();
        ms.translate(buttonX, buttonY, 0);

        boolean isHovered = mouseX >= buttonX && mouseX <= buttonX + 18 &&
                mouseY >= scrolledY && mouseY <= scrolledY + 18 &&
                scrolledY > guiTop + 16 && scrolledY < guiTop + 189;

        if (isHovered) {
            graphics.fill(0, 0, 18, 18, 0x22000000);
        }

        icon.render(graphics, 1, 1);
        ms.popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;

        double scrollVal = scroll.getValue(0);
        int startX = guiLeft + 25;
        int startY = guiTop + 25;

        for (int i = 0; i < be.connections.size(); i++) {
            double cardY = startY + i * (CARD_HEADER + 4) - scrollVal;

            if (cardY + CARD_HEADER < guiTop + 16 || cardY > guiTop + 189) continue;

            if (mouseX >= startX && mouseX <= startX + CARD_WIDTH &&
                    mouseY >= cardY && mouseY <= cardY + CARD_HEADER) {

                ScreenOpener.open(new SmartDisplayLinkScreen(be, i));
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f) {
        if (super.mouseScrolled(d, e, f)) return true;

        float contentHeight = be.connections.size() * (CARD_HEADER + 4);
        float visibleHeight = 173;
        float maxScroll = Math.max(0, contentHeight - visibleHeight + 20);

        if (maxScroll > 0) {
            float chaseTarget = scroll.getChaseTarget();
            chaseTarget -= f * 18;
            chaseTarget = Mth.clamp(chaseTarget, 0, maxScroll);
            scroll.chase(chaseTarget, 0.7f, Chaser.EXP);
            return true;
        }

        return false;
    }
}