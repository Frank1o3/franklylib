package com.frank1o3.franklylib.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyle;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyles;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * Wraps {@link FranklyGuiUtils#drawScaledEntityPreview} as a real widget
 * instead of a static call a screen has to remember to invoke in the right
 * place.
 *
 * <p>
 * Interaction:
 * <ul>
 * <li>Scroll wheel while hovered — zooms the preview in/out.</li>
 * <li>Left-click + drag — pans the preview within its box.</li>
 * <li>Right-click + drag — orbits the entity (as before); releasing returns
 * to the default "face the cursor" behaviour.</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public class FranklyEntityPreviewWidget extends AbstractWidget {

    /**
     * GLFW button indices, matching this codebase's other event-based input
     * handling.
     */
    private static final int BUTTON_LEFT = 0;
    private static final int BUTTON_RIGHT = 1;

    private static final float MIN_ZOOM = 0.35f;
    private static final float MAX_ZOOM = 3.0f;
    private static final float ZOOM_STEP = 0.1f;

    /** Clamp so panning can't drag the entity fully out of the widget's box. */
    private static final float MAX_PAN_FRACTION = 0.65f;

    private final Supplier<LivingEntity> entitySupplier;
    private final int basePreviewSize;
    private float zoom = 1.0f;
    private float orbitYaw = 0f;
    private float panX = 0f;
    private float panY = 0f;

    private boolean panning;
    private boolean orbiting;

    private final @Nullable Identifier style;
    private final @Nullable Float lockedScale;

    private FranklyEntityPreviewWidget(int x, int y, int width, int height, int previewSize,
            Supplier<LivingEntity> entitySupplier, @Nullable Identifier style, @Nullable Float lockedScale) {
        super(x, y, width, height, Component.empty());
        this.basePreviewSize = previewSize;
        this.entitySupplier = entitySupplier;
        this.style = style;
        this.lockedScale = lockedScale;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        LivingEntity entity = entitySupplier.get();
        FranklyUiStyles.resolve(style, new FranklyUiStyle(0x33000000, 0x33000000, 0x33000000, 0,
                0xFFFFFFFF, 0xFFFFFFFF, 0, 0, FranklyUiStyle.BorderType.NONE, 0, 0))
                .drawBox(graphics, getX(), getY(), width, height, isHoveredOrFocused(), active);
        if (entity == null) {
            net.minecraft.client.gui.Font font = net.minecraft.client.Minecraft.getInstance().font;
            Component noEntityText = Component.literal("—");
            graphics.text(font, noEntityText, getX() + (width - font.width(noEntityText)) / 2,
                    getY() + (height - font.lineHeight) / 2, 0x88FFFFFF, false);
            return;
        }

        // Pan is expressed as a fraction of the box size, clamped so the preview
        // can't be dragged fully outside its own bounds.
        int maxPanX = (int) (width * MAX_PAN_FRACTION);
        int maxPanY = (int) (height * MAX_PAN_FRACTION);
        int offsetX = (int) Mth.clamp(panX, -maxPanX, maxPanX);
        int offsetY = (int) Mth.clamp(panY, -maxPanY, maxPanY);

        int x1 = getX() + offsetX;
        int y1 = getY() + offsetY;
        int x2 = getX() + width + offsetX;
        int y2 = getY() + height + offsetY;
        int effectiveSize = Math.max(1, Math.round(basePreviewSize * zoom));

        // Orbiting works by feeding a shifted fake cursor X into the "face the
        // mouse" math the helper already does — cheap, and keeps
        // FranklyGuiUtils untouched. When not orbiting, the real cursor drives
        // it directly, preserving the original "faces you" default behaviour.
        float fakeMouseX = mouseX - orbitYaw * 4f;
        graphics.enableScissor(getX(), getY(), getX() + width, getY() + height);
        FranklyGuiUtils.drawScaledEntityPreview(graphics, x1, y1, x2, y2, effectiveSize, fakeMouseX, mouseY, entity,
                lockedScale);
        graphics.disableScissor();
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == BUTTON_RIGHT) {
            orbiting = true;
            panning = false;
        } else if (event.button() == BUTTON_LEFT) {
            panning = true;
            orbiting = false;
        }
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        if (orbiting) {
            orbitYaw += (float) dragX;
        } else if (panning) {
            panX += (float) dragX;
            panY += (float) dragY;
        }
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        panning = false;
        orbiting = false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (!active || !isHovered()) {
            return false;
        }
        zoom = Mth.clamp(zoom + (float) vertical * ZOOM_STEP, MIN_ZOOM, MAX_ZOOM);
        return true;
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
        private @Nullable Float lockedScale;

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

        /**
         * Locks the preview to a fixed scale, independent of the entity's actual
         * current size. 1.0 = the entity's unscaled default; e.g. 1.2 = 20%
         * bigger, 0.5 = half size. Omit this call to have the preview reflect
         * the entity's real, live scale (the previous/default behaviour).
         */
        public Builder lockedScale(float lockedScale) {
            this.lockedScale = lockedScale;
            return this;
        }

        public FranklyEntityPreviewWidget build() {
            return new FranklyEntityPreviewWidget(x, y, width, height, previewSize, entitySupplier, style, lockedScale);
        }
    }
}