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
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyle;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyles;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class FranklyDropdown<T> extends AbstractWidget {
    private final List<T> options;
    private final Function<T, Component> labelMapper;
    private final Consumer<T> onSelect;
    private T current;
    private boolean expanded;
    private float expansionProgress;
    private long expansionUpdatedAt = System.nanoTime();
    private final @Nullable Identifier animation;
    private final @Nullable Identifier style;

    private FranklyDropdown(int x, int y, int width, int height, List<T> options, T current,
            Function<T, Component> labelMapper, Consumer<T> onSelect, @Nullable Identifier style,
            @Nullable Identifier animation) {
        super(x, y, width, height, Component.empty());
        this.options = List.copyOf(options);
        this.current = current;
        this.labelMapper = labelMapper != null ? labelMapper : value -> Component.literal(String.valueOf(value));
        this.onSelect = onSelect != null ? onSelect : value -> {
        };
        this.style = style;
        this.animation = animation;
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
        FranklyButton.builder()
                .bounds(getX(), getY(), getWidth(), getHeight())
                .message(labelMapper.apply(current))
                .onPress(btn -> expanded = !expanded)
                .style(style)
                .animation(animation)
                .build()
                .extractContents(graphics, mouseX, mouseY, delta);
        Font font = Minecraft.getInstance().font;
        FranklyUiStyle uiStyle = FranklyUiStyles.resolve(style, FranklyUiStyle.DEFAULT);
        graphics.text(font, "▾", getX() + getWidth() - 14, getY() + (getHeight() - font.lineHeight) / 2, uiStyle.text(active),
                false);
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

        /** Applies the same optional transform animation used by buttons. */
        public Builder<T> animation(@Nullable Identifier animation) {
            this.animation = animation;
            return this;
        }

        public FranklyDropdown<T> build() {
            return new FranklyDropdown<>(x, y, width, height, options, current, labelMapper, onSelect, style, animation);
        }
    }
}
