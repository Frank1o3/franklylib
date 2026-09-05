package com.frank1o3.franklylib.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.frank1o3.franklylib.client.gui.animation.FranklyUiAnimation;
import com.frank1o3.franklylib.client.gui.animation.FranklyUiAnimations;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyle;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyles;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class FranklyDropdown<T> extends AbstractWidget implements FranklyDepthAware {
    private final List<T> options;
    private final Function<T, Component> labelMapper;
    private final Consumer<T> onSelect;
    private T current;
    private boolean expanded;
    private float expansionProgress;
    private long expansionUpdatedAt = System.nanoTime();
    private final @Nullable Identifier animation;
    private final @Nullable Identifier style;
    private int zIndex;

    private FranklyDropdown(int x, int y, int width, int height, List<T> options, T current,
            Function<T, Component> labelMapper, Consumer<T> onSelect, @Nullable Identifier style,
            @Nullable Identifier animation, int zIndex) {
        super(x, y, width, height, Component.empty());
        this.options = List.copyOf(options);
        this.current = current;
        this.labelMapper = labelMapper != null ? labelMapper : value -> Component.literal(String.valueOf(value));
        this.onSelect = onSelect != null ? onSelect : value -> {
        };
        this.style = style;
        this.animation = animation;
        this.zIndex = zIndex;
    }

    public T getCurrent() {
        return current;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!isActive()) {
            return false;
        }
        if (expanded) {
            int optionHeight = Math.max(14, getHeight());
            int listHeight = Math.min(options.size() * optionHeight, 140);
            return mouseX >= getX() && mouseX < getRight()
                    && mouseY >= getY() && mouseY < getY() + getHeight() + listHeight;
        }
        return super.isMouseOver(mouseX, mouseY);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        updateExpansion();
        FranklyUiAnimation frame = FranklyUiAnimations.beginTransform(graphics, this, animation,
                isHoveredOrFocused(), active, getX() + getWidth() / 2f, getY() + getHeight() / 2f);

        FranklyUiStyle uiStyle = FranklyUiStyles.resolve(style, FranklyUiStyle.DEFAULT);
        uiStyle.withAlpha(frame.alpha()).drawBox(graphics, getX(), getY(), getWidth(), getHeight(), isHoveredOrFocused(), active);

        Font font = Minecraft.getInstance().font;
        int textColor = FranklyUiAnimations.applyAlpha(uiStyle.text(active), frame.alpha());
        int left = getX() + uiStyle.padding();
        int right = getX() + getWidth() - uiStyle.padding() - 14;
        FranklyGuiUtils.drawFittedText(FranklyGuiUtils.Justify.LEFT, graphics, font, labelMapper.apply(current),
                left, getY(), right, getY() + getHeight(), textColor);

        graphics.text(font, "▾", getX() + getWidth() - 14, getY() + (getHeight() - font.lineHeight) / 2, textColor,
                false);

        if (isHovered()) {
            graphics.requestCursor(active ? CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
        }
        FranklyUiAnimations.endTransform(graphics);
        if (expansionProgress > 0.001f) {
            int optionHeight = Math.max(14, getHeight());
            int listHeight = Math.min(options.size() * optionHeight, 140);
            int listY = getY() + getHeight() + Math.round(4 * (1f - expansionProgress));
            int visibleHeight = Math.max(1, Math.round(listHeight * expansionProgress));
            // A soft offset shadow keeps the popup visually separate from widgets below it.
            graphics.fill(getX() + 2, listY + 3, getX() + getWidth() + 2, listY + visibleHeight + 3,
                    alpha(0x70000000, expansionProgress));
            graphics.enableScissor(getX() - 2, listY, getX() + getWidth() + 2, listY + visibleHeight);
            uiStyle.withAlpha(expansionProgress)
                    .drawBox(graphics, getX(), listY, getWidth(), listHeight, false, active);
            for (int i = 0; i < options.size(); i++) {
                T option = options.get(i);
                int y = listY + i * optionHeight;
                int bg = (mouseX >= getX() && mouseX <= getX() + getWidth() && mouseY >= y && mouseY < y + optionHeight)
                        ? uiStyle.hoverBackground() : uiStyle.background();
                graphics.fill(getX() + 1, y, getX() + getWidth() - 1, y + optionHeight,
                        alpha(bg, expansionProgress));
                graphics.text(font, labelMapper.apply(option), getX() + uiStyle.padding(), y + 3,
                        alpha(uiStyle.text(active), expansionProgress), false);
            }
            graphics.disableScissor();
        }
    }

    private void updateExpansion() {
        long now = System.nanoTime();
        float elapsed = Math.min(1f, (now - expansionUpdatedAt) / 140_000_000f);
        expansionUpdatedAt = now;
        float target = expanded ? 1f : 0f;
        // Ease out on both directions: responsive at the start, settled at the end.
        float progress = 1f - (1f - elapsed) * (1f - elapsed);
        expansionProgress += (target - expansionProgress) * progress;
    }

    private static int alpha(int color, float alpha) {
        int base = color >>> 24;
        return (Math.round(base * Math.clamp(alpha, 0f, 1f)) << 24) | (color & 0x00FFFFFF);
    }

    @Override
    public int getZIndex() {
        // When expanded or animating, pop up into the overlay layer (+1000) so the dropdown list draws on top
        if (expanded || expansionProgress > 0.001f) {
            return zIndex + 1000;
        }
        return zIndex;
    }

    @Override
    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
    }

    public int getBaseZIndex() {
        return zIndex;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!this.active || !this.visible) {
            return false;
        }
        // If clicking outside an expanded dropdown, close it and let the click interact with the underlying target
        if (expanded && !isMouseOver(event.x(), event.y())) {
            expanded = false;
            return false;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        // Click landed on an open option item -> select it
        if (expanded && mouseY >= getBottom()) {
            int optionHeight = Math.max(14, getHeight());
            int index = (int) ((mouseY - getBottom()) / optionHeight);
            if (index >= 0 && index < options.size()
                    && mouseX >= getX() && mouseX < getRight()) {
                current = options.get(index);
                onSelect.accept(current);
            }
            expanded = false;
            return;
        }

        // Click on main button -> toggle dropdown
        if (mouseX >= getX() && mouseX < getRight() && mouseY >= getY() && mouseY < getBottom()) {
            expanded = !expanded;
        } else {
            expanded = false;
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static final class Builder<T> {
        private int x;
        private int y;
        private int width = 120;
        private int height = 20;
        private List<T> options = List.of();
        private T current;
        private @Nullable Function<T, Component> labelMapper;
        private @Nullable Consumer<T> onSelect;
        private @Nullable Identifier style;
        private @Nullable Identifier animation;
        private @Nullable Integer zIndex;

        public Builder<T> bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder<T> options(List<T> options) {
            this.options = options;
            return this;
        }

        public Builder<T> current(T current) {
            this.current = current;
            return this;
        }

        public Builder<T> labelMapper(Function<T, Component> labelMapper) {
            this.labelMapper = labelMapper;
            return this;
        }

        public Builder<T> onSelect(Consumer<T> onSelect) {
            this.onSelect = onSelect;
            return this;
        }

        public Builder<T> style(@Nullable Identifier style) {
            this.style = style;
            return this;
        }

        /** Sets the base z-index depth of this dropdown. */
        public Builder<T> zIndex(int zIndex) {
            this.zIndex = zIndex;
            return this;
        }

        /** Applies the same optional transform animation used by buttons. */
        public Builder<T> animation(@Nullable Identifier animation) {
            this.animation = animation;
            return this;
        }

        public FranklyDropdown<T> build() {
            int resolvedZIndex = this.zIndex != null ? this.zIndex
                    : (style != null ? FranklyUiStyles.resolve(style, FranklyUiStyle.DEFAULT).zIndex() : 0);
            return new FranklyDropdown<>(x, y, width, height, options, current, labelMapper, onSelect, style, animation, resolvedZIndex);
        }
    }
}
