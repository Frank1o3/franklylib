package com.frank1o3.franklylib.client.gui;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.frank1o3.franklylib.client.gui.animation.FranklyUiAnimation;
import com.frank1o3.franklylib.client.gui.animation.FranklyUiAnimations;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyle;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyles;
import org.jetbrains.annotations.Nullable;

/**
 * A flat-style button matching the look of {@link FranklySlider}, so buttons
 * and
 * sliders in the same panel feel like one cohesive widget set instead of
 * mixing vanilla's beveled button texture with a custom slider.
 *
 * <p>
 * Construct via {@link #builder()}:
 *
 * <pre>{@code
 * FranklyButton reset = FranklyButton.builder()
 *         .bounds(x, y, 86, 20)
 *         .message(Component.translatable("gui.proportionality.scale.reset"))
 *         .onPress(btn -> ClientScaleNetwork.sendResetRequest())
 *         .build();
 * }</pre>
 */
@Environment(EnvType.CLIENT)
public class FranklyButton extends Button {

    private static final int COLOR_BG = 0x54_444444;
    private static final int COLOR_BG_HOVER = 0x54_666666;
    private static final int COLOR_BG_DISABLED = 0x54_222222;
    private static final int COLOR_TEXT = 0xFF_FFFFFF;
    private static final int COLOR_TEXT_DISABLED = 0xFF_666666;
    private final @Nullable Identifier animation;
    private final @Nullable Identifier style;

    private FranklyButton(int x, int y, int width, int height, Component message, OnPress onPress,
            @Nullable Identifier animation, @Nullable Identifier style) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.animation = animation;
        this.style = style;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        FranklyUiAnimation frame = FranklyUiAnimations.beginTransform(graphics, this, animation,
                isHoveredOrFocused(), active, getX() + getWidth() / 2f, getY() + getHeight() / 2f);

        FranklyUiStyle uiStyle = FranklyUiStyles.resolve(style,
                new FranklyUiStyle(COLOR_BG, COLOR_BG_HOVER, COLOR_BG_DISABLED, 0, COLOR_TEXT, COLOR_TEXT_DISABLED, 0, 2,
                        FranklyUiStyle.BorderType.NONE, 0, 0));
        uiStyle.withAlpha(frame.alpha()).drawBox(graphics, getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused(), active);

        Font font = Minecraft.getInstance().font;
        int textColor = FranklyUiAnimations.applyAlpha(uiStyle.text(active), frame.alpha());
        int left = getX() + uiStyle.padding();
        int right = getX() + getWidth() - uiStyle.padding();
        FranklyGuiUtils.drawFittedText(FranklyGuiUtils.Justify.CENTER, graphics, font, getMessage(),
                left, getY(), right, getY() + getHeight(), textColor);

        if (isHovered()) {
            graphics.requestCursor(active ? CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
        }
        FranklyUiAnimations.endTransform(graphics);
    }

    // =========================================================================
    // Builder
    // =========================================================================

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("UnusedReturnValue")
    public static final class Builder {
        private int x, y, width = 86, height = 20;
        private @Nullable Component message;
        private @Nullable OnPress onPress;
        private boolean active = true;
        private @Nullable Identifier animation;
        private @Nullable Identifier style;

        private Builder() {
        }

        public Builder bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder message(Component message) {
            this.message = message;
            return this;
        }

        public Builder onPress(OnPress onPress) {
            this.onPress = onPress;
            return this;
        }

        public Builder active(boolean active) {
            this.active = active;
            return this;
        }

        /**
         * Opts this button into a resource-pack UI animation, e.g.
         * {@code modid:gentle_hover}.
         */
        public Builder animation(@Nullable Identifier animation) {
            this.animation = animation;
            return this;
        }

        /** Applies a resource-pack style, e.g. {@code modid:primary_button}. */
        public Builder style(@Nullable Identifier style) {
            this.style = style;
            return this;
        }

        public FranklyButton build() {
            Component msg = message != null ? message : Component.empty();
            OnPress press = onPress != null ? onPress : btn -> {
            };
            FranklyButton button = new FranklyButton(x, y, width, height, msg, press, animation, style);
            button.active = active;
            return button;
        }
    }
}
