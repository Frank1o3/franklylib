package com.frank1o3.franklylib.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyle;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyles;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Wraps {@link FranklyGuiUtils#drawScaledEntityPreview} as a real widget
 * instead of a
 * static call a screen has to remember to invoke in the right place. Adds
 * click-drag
 * orbiting on top, which the underlying helper doesn't have on its own — it
 * only ever
 * faces the raw cursor position.
 */
@Environment(EnvType.CLIENT)
public class FranklyEntityPreviewWidget extends AbstractWidget {
    private final Supplier<LivingEntity> entitySupplier;
    private final int previewSize;
    private float orbitYaw = 0f;
    private boolean dragging;
    private final @Nullable Identifier style;

    private FranklyEntityPreviewWidget(int x, int y, int width, int height, int previewSize,
            Supplier<LivingEntity> entitySupplier, @Nullable Identifier style) {
        super(x, y, width, height, Component.empty());
        this.previewSize = previewSize;
        this.entitySupplier = entitySupplier;
        this.style = style;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        LivingEntity entity = entitySupplier.get();
        FranklyUiStyles.resolve(style, new FranklyUiStyle(0x33000000, 0x33000000, 0x33000000, 0,
                0xFFFFFFFF, 0xFFFFFFFF, 0, 0, FranklyUiStyle.BorderType.NONE, 0, 0))
                .drawBox(graphics, getX(), getY(), width, height, isHoveredOrFocused(), active);
        if (entity == null) {
            return;
        }
        // Orbiting works by feeding a shifted fake cursor X into the "face the mouse"
        // math the helper already does — cheap, and keeps FranklyGuiUtils untouched.
        float fakeMouseX = mouseX - orbitYaw * 4f;
        FranklyGuiUtils.drawScaledEntityPreview(graphics, getX(), getY(), getX() + width, getY() + height,
                previewSize, fakeMouseX, mouseY, entity);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        dragging = true;
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        if (dragging) {
            orbitYaw += (float) dragX;
        }
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        dragging = false;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int x, y, width = 80, height = 100, previewSize = 30;
        private Supplier<LivingEntity> entitySupplier = () -> null;
        private @Nullable Identifier style;

        public Builder bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder previewSize(int previewSize) {
            this.previewSize = previewSize;
            return this;
        }

        public Builder entity(Supplier<LivingEntity> entitySupplier) {
            this.entitySupplier = entitySupplier;
            return this;
        }

        public Builder style(@Nullable Identifier style) {
            this.style = style;
            return this;
        }

        public FranklyEntityPreviewWidget build() {
            return new FranklyEntityPreviewWidget(x, y, width, height, previewSize, entitySupplier, style);
        }
    }
}
