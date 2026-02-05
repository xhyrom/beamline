package dev.xhyrom.beamline.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.behaviour.display.DisplayTarget;
import com.simibubi.create.api.registry.CreateBuiltInRegistries;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.ModularGuiLine;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import com.simibubi.create.foundation.gui.widget.IconButton;
import com.simibubi.create.foundation.gui.widget.Label;
import com.simibubi.create.foundation.gui.widget.ScrollInput;
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput;
import com.simibubi.create.foundation.utility.CreateLang;
import com.simibubi.create.infrastructure.ponder.AllCreatePonderTags;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import dev.xhyrom.beamline.block.SmartDisplayLinkBlock;
import dev.xhyrom.beamline.block.entity.SmartDisplayLinkBlockEntity;
import dev.xhyrom.beamline.network.SmartDisplayLinkConfigurationPacket;
import dev.xhyrom.beamline.util.SmartDisplayLinkContext;
import net.createmod.catnip.data.Couple;
import net.createmod.catnip.gui.AbstractSimiScreen;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.gui.UIRenderHelper;
import net.createmod.catnip.gui.element.GuiGameElement;
import net.createmod.catnip.gui.widget.AbstractSimiWidget;
import net.createmod.catnip.gui.widget.ElementWidget;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.ponder.foundation.ui.PonderTagScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SmartDisplayLinkScreen extends AbstractSimiScreen {
    private static final ItemStack FALLBACK = new ItemStack(Items.BARRIER);

    private final AllGuiTextures background;
    private final SmartDisplayLinkBlockEntity blockEntity;
    private final int connectionIndex;

    private IconButton confirmButton;

    BlockState sourceState;
    BlockState targetState;
    List<DisplaySource> sources;
    DisplayTarget target;

    ScrollInput sourceTypeSelector;
    Label sourceTypeLabel;
    ScrollInput targetLineSelector;
    Label targetLineLabel;
    AbstractSimiWidget sourceWidget;
    AbstractSimiWidget targetWidget;
    private EditBox nameInput;

    Couple<ModularGuiLine> configWidgets;

    public SmartDisplayLinkScreen(SmartDisplayLinkBlockEntity be, int connectionIndex) {
        this.background = AllGuiTextures.DATA_GATHERER;
        this.blockEntity = be;
        this.connectionIndex = connectionIndex;
        this.sources = Collections.emptyList();
        this.configWidgets = Couple.create(ModularGuiLine::new);
        this.target = null;
    }

    private @NotNull SmartDisplayLinkBlockEntity.LinkConnection getConnection() {
        return blockEntity.connections.get(connectionIndex);
    }

    private BlockPos getTargetPos() {
        return blockEntity.targetPosition(getConnection());
    }

    private CompoundTag getCurrentSourceConfig() {
        SmartDisplayLinkBlockEntity.LinkConnection conn = getConnection();
        return conn.sourceConfig();
    }

    private DisplaySource getCurrentSource() {
        SmartDisplayLinkBlockEntity.LinkConnection conn = getConnection();
        return conn.source();
    }

    private int getCurrentTargetLine() {
        SmartDisplayLinkBlockEntity.LinkConnection conn = getConnection();
        return conn.targetLine();
    }

    @Override
    protected void init() {
        setWindowSize(background.getWidth(), background.getHeight());
        super.init();
        clearWidgets();

        initGathererOptions();

        int x = guiLeft;
        int y = guiTop;
        int h = background.getHeight();

        confirmButton = new IconButton(x + background.getWidth() - 33, y + h - 24, AllIcons.I_CONFIRM);
        confirmButton.withCallback(this::onConfirm);
        addRenderableWidget(confirmButton);

        nameInput = new EditBox(font, x + 38 + 5, y + h - 24 + 4, 115 - 10, 10, Component.literal("Name"));
        nameInput.setBordered(false);
        nameInput.setTextColor(0xffffff);
        if (getConnection().name() != null) {
            nameInput.setValue(getConnection().name());
        }
        nameInput.setTooltip(Tooltip.create(
                Component.translatable("create_beamline.screen.smart_display_link.name_tooltip.title").withColor(AbstractSimiWidget.HEADER_RGB.getRGB())
                        .append(Component.literal("\n"))
                        .append(Component.translatable("create_beamline.screen.smart_display_link.name_tooltip.display").withStyle(ChatFormatting.DARK_GRAY))
                )
        );
        addRenderableWidget(nameInput);
    }

    @Override
    public void tick() {
        super.tick();

        if (sourceState != null && sourceState.getBlock() != minecraft.level.getBlockState(blockEntity.sourcePosition()).getBlock()) {
            initGathererOptions();
        }

        if (targetState != null && targetState.getBlock() != minecraft.level.getBlockState(getTargetPos()).getBlock()) {
            initGathererOptions();
        }
    }

    @SuppressWarnings("deprecation")
    private void initGathererOptions() {
        ClientLevel level = minecraft.level;
        BlockPos sourcePos = blockEntity.sourcePosition();
        BlockPos targetPos = getTargetPos();

        sourceState = level.getBlockState(sourcePos);
        targetState = level.getBlockState(targetPos);

        ItemStack asItem;
        int x = guiLeft;
        int y = guiTop;

        Block sourceBlock = sourceState.getBlock();
        Block targetBlock = targetState.getBlock();

        asItem = sourceBlock.getCloneItemStack(level, sourcePos, sourceState);
        ItemStack sourceIcon = asItem == null || asItem.isEmpty() ? FALLBACK : asItem;
        asItem = targetBlock.getCloneItemStack(level, targetPos, targetState);
        ItemStack targetIcon = asItem == null || asItem.isEmpty() ? FALLBACK : asItem;

        sources = DisplaySource.getAll(level, sourcePos);
        target = DisplayTarget.get(level, targetPos);

        removeWidget(targetLineSelector);
        removeWidget(targetLineLabel);
        removeWidget(sourceTypeSelector);
        removeWidget(sourceTypeLabel);
        removeWidget(sourceWidget);
        removeWidget(targetWidget);

        configWidgets.forEach(s -> s.forEach(this::removeWidget));

        targetLineSelector = null;
        sourceTypeSelector = null;

        if (target != null) {
            DisplayTargetStats stats = target.provideStats(new SmartDisplayLinkContext(level, blockEntity, getConnection()));
            int rows = stats.maxRows();
            int startIndex = Math.min(getCurrentTargetLine(), rows);

            targetLineLabel = new Label(x + 65, y + 109, CommonComponents.EMPTY).withShadow();
            targetLineLabel.text = target.getLineOptionText(startIndex);

            if (rows > 1) {
                targetLineSelector = new ScrollInput(x + 61, y + 105, 135, 16).withRange(0, rows)
                        .titled(CreateLang.translateDirect("display_link.display_on"))
                        .inverted()
                        .calling(i -> targetLineLabel.text = target.getLineOptionText(i))
                        .setState(startIndex);
                addRenderableWidget(targetLineSelector);
            }

            addRenderableWidget(targetLineLabel);
        }

        sourceWidget = new ElementWidget(x + 37, y + 26)
                .showingElement(GuiGameElement.of(sourceIcon))
                .withCallback((mX, mY) -> ScreenOpener.open(new PonderTagScreen(AllCreatePonderTags.DISPLAY_SOURCES)));

        sourceWidget.getToolTip().addAll(List.of(
                CreateLang.translateDirect("display_link.reading_from"),
                sourceState.getBlock().getName()
                        .withStyle(s -> s.withColor(sources.isEmpty() ? 0xF68989 : 0xF2C16D)),
                CreateLang.translateDirect("display_link.attached_side"),
                CreateLang.translateDirect("display_link.view_compatible")
                        .withStyle(ChatFormatting.GRAY)
        ));

        addRenderableWidget(sourceWidget);

        targetWidget = new ElementWidget(x + 37, y + 105)
                .showingElement(GuiGameElement.of(targetIcon))
                .withCallback((mX, mY) -> {
                    ScreenOpener.open(new PonderTagScreen(AllCreatePonderTags.DISPLAY_TARGETS));
                });

        targetWidget.getToolTip().addAll(List.of(
                CreateLang.translateDirect("display_link.writing_to"),
                targetState.getBlock().getName()
                        .withStyle(s -> s.withColor(target == null ? 0xF68989 : 0xF2C16D)),
                CreateLang.translateDirect("display_link.targeted_location"),
                CreateLang.translateDirect("display_link.view_compatible")
                        .withStyle(ChatFormatting.GRAY)
        ));

        addRenderableWidget(targetWidget);

        if (!sources.isEmpty()) {
            int startIndex = Math.max(sources.indexOf(getCurrentSource()), 0);

            sourceTypeLabel = new Label(x + 65, y + 30, CommonComponents.EMPTY).withShadow();
            sourceTypeLabel.text = sources.get(startIndex).getName();

            if (sources.size() > 1) {
                List<Component> options = sources.stream()
                        .map(DisplaySource::getName)
                        .toList();
                sourceTypeSelector = new SelectionScrollInput(x + 61, y + 26, 135, 16).forOptions(options)
                        .writingTo(sourceTypeLabel)
                        .titled(CreateLang.translateDirect("display_link.information_type"))
                        .calling(this::initGathererSourceSubOptions)
                        .setState(startIndex);
                sourceTypeSelector.onChanged();
                addRenderableWidget(sourceTypeSelector);
            } else {
                initGathererSourceSubOptions(0);
            }

            addRenderableWidget(sourceTypeLabel);
        }
    }

    private void initGathererSourceSubOptions(int i) {
        DisplaySource source = sources.get(i);
        source.populateData(new SmartDisplayLinkContext(blockEntity.getLevel(), blockEntity, getConnection()));

        if (targetLineSelector != null)
            targetLineSelector
                    .titled(source instanceof SingleLineDisplaySource ? CreateLang.translateDirect("display_link.display_on")
                            : CreateLang.translateDirect("display_link.display_on_multiline"));

        configWidgets.forEach(s -> {
            s.forEach(this::removeWidget);
            s.clear();
        });

        DisplayLinkContext context = new SmartDisplayLinkContext(minecraft.level, blockEntity, getConnection());

        configWidgets.forEachWithContext((s, first) -> source.initConfigurationWidgets(context,
                new ModularGuiLineBuilder(font, s, guiLeft + 60, guiTop + (first ? 51 : 72)), first));

        configWidgets.forEach(s -> s.loadValues(getCurrentSourceConfig(), this::addRenderableWidget, this::addRenderableOnly));
    }

    public void onConfirm() {
        onConfirm(true);
    }

    public void onConfirm(boolean backToSelection) {
        CompoundTag sourceData = new CompoundTag();

        if (!sources.isEmpty()) {
            DisplaySource source = sources.get(sourceTypeSelector == null ? 0 : sourceTypeSelector.getState());
            ResourceLocation id = CreateBuiltInRegistries.DISPLAY_SOURCE.getKey(source);
            if (id != null) {
                sourceData.putString("Id", id.toString());
            }
            configWidgets.forEach(s -> s.saveValues(sourceData));
        }

        CatnipServices.NETWORK.sendToServer(new SmartDisplayLinkConfigurationPacket(
                blockEntity.getBlockPos(),
                Optional.ofNullable(nameInput.getValue().isEmpty() ? null : nameInput.getValue()),
                connectionIndex,
                sourceData,
                targetLineSelector == null ? 0 : targetLineSelector.getState()
        ));

        if (backToSelection) {
            ScreenOpener.open(new SmartDisplayLinkSelectionScreen(blockEntity));
        }
    }

    @Override
    public void onClose() {
        super.onClose();
        onConfirm(false);
    }

    @Override
    protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        int x = guiLeft;
        int y = guiTop;
        int h = background.getHeight();

        background.render(graphics, x, y);

        MutableComponent header = Component.translatable("create_beamline.screen.smart_display_link.title");
        header.append(Component.literal(" #" + (connectionIndex + 1)));

        graphics.drawString(font, header, x + background.getWidth() / 2 - font.width(header) / 2, y + 4, 0x592424, false);

        if (sources.isEmpty())
            graphics.drawString(font, CreateLang.translateDirect("display_link.no_source"), x + 65, y + 30, 0xD3D3D3);
        if (target == null)
            graphics.drawString(font, CreateLang.translateDirect("display_link.no_target"), x + 65, y + 109, 0xD3D3D3);

        PoseStack ms = graphics.pose();

        ms.pushPose();
        ms.translate(0, guiTop + 46, 0);
        configWidgets.getFirst().renderWidgetBG(guiLeft, graphics);
        ms.translate(0, 21, 0);
        configWidgets.getSecond().renderWidgetBG(guiLeft, graphics);
        ms.popPose();

        UIRenderHelper.drawStretched(graphics, x + 38, y + h - 24, 115, 18, 0, AllGuiTextures.DATA_AREA);
        AllGuiTextures.DATA_AREA_START.render(graphics, x + 38, y + h - 24);
        AllGuiTextures.DATA_AREA_END.render(graphics, x + 37 + 116, y + h - 24);

        ms.pushPose();
        TransformStack.of(ms)
                .pushPose()
                .translate(x + background.getWidth() + 4, y + h + 4, 100)
                .scale(40)
                .rotateXDegrees(-22)
                .rotateYDegrees(63);
        GuiGameElement.of(blockEntity.getBlockState()
                        .setValue(SmartDisplayLinkBlock.FACING, Direction.UP))
                .render(graphics);
        ms.popPose();
    }
}