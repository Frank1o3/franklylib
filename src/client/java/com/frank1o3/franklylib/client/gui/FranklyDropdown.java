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

    private FranklyDropdown(int x, int y, int width, int height, List<T> options, T current,
            Function<T, Component> labelMapper, Consumer<T> onSelect) {
        super(x, y, width, height, Component.empty());
        this.options = List.copyOf(options);
        this.current = current;
        this.labelMapper = labelMapper != null ? labelMapper : value -> Component.literal(String.valueOf(value));
        this.onSelect = onSelect != null ? onSelect : value -> {
        };
    }

    public T getCurrent() {
        return current;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        FranklyButton.builder()
                .bounds(getX(), getY(), getWidth(), getHeight())
                .message(labelMapper.apply(current))
                .onPress(btn -> expanded = !expanded)
                .build()
                .extractContents(graphics, mouseX, mouseY, delta);
        Font font = Minecraft.getInstance().font;
        graphics.text(font, "▾", getX() + getWidth() - 14, getY() + (getHeight() - font.lineHeight) / 2, 0xFF_FFFFFF,
                false);
        if (expanded) {
            int optionHeight = Math.max(14, getHeight());
            int listHeight = Math.min(options.size() * optionHeight, 140);
            graphics.fill(getX(), getY() + getHeight(), getX() + getWidth(), getY() + getHeight() + listHeight,
                    0xCC_222233);
            for (int i = 0; i < options.size(); i++) {
                T option = options.get(i);
                int y = getY() + getHeight() + i * optionHeight;
                graphics.fill(getX(), y, getX() + getWidth(), y + optionHeight, 0x54_444444);
                graphics.text(font, labelMapper.apply(option), getX() + 4, y + 3, 0xFF_FFFFFF, false);
            }
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();

        // Click landed on an already-open option list -> try to select it.
        if (expanded && mouseY >= getY() + getHeight()) {
            int optionHeight = Math.max(14, getHeight());
            int index = (int) ((mouseY - (getY() + getHeight())) / optionHeight);
            if (index >= 0 && index < options.size()
                    && mouseX >= getX() && mouseX <= getX() + getWidth()) {
                current = options.get(index);
                onSelect.accept(current);
            }
            expanded = false;
            return;
        }

        // Click on the main button area -> toggle open/closed.
        expanded = !expanded;
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

        public FranklyDropdown<T> build() {
            return new FranklyDropdown<>(x, y, width, height, options, current, labelMapper, onSelect);
        }
    }
}