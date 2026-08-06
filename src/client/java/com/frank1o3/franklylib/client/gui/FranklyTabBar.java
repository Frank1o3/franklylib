package com.frank1o3.franklylib.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class FranklyTabBar<T> extends AbstractWidget {
    private final List<T> tabs;
    private final Function<T, Component> labelMapper;
    private final Consumer<T> onSelect;
    private final List<FranklyButton> buttons = new ArrayList<>();
    private T current;

    private FranklyTabBar(int x, int y, int width, int height, List<T> tabs, Function<T, Component> labelMapper,
            T current, Consumer<T> onSelect) {
        super(x, y, width, height, Component.empty());
        this.tabs = new ArrayList<>(tabs);
        this.labelMapper = labelMapper != null ? labelMapper : tab -> Component.literal(String.valueOf(tab));
        this.current = current;
        this.onSelect = onSelect != null ? onSelect : value -> {
        };
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        int segmentWidth = tabs.isEmpty() ? width : width / tabs.size();
        for (int i = 0; i < tabs.size(); i++) {
            int x = getX() + i * segmentWidth;
            int tabWidth = (i == tabs.size() - 1) ? (getX() + width - x) : segmentWidth;
            if (event.x() >= x && event.x() < x + tabWidth && event.y() >= getY() && event.y() < getY() + getHeight()) {
                T tab = tabs.get(i);
                if (!tab.equals(current)) {
                    this.current = tab;
                    this.onSelect.accept(tab);
                    return;
                }
            }
        }
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        buttons.clear();
        int segmentWidth = tabs.isEmpty() ? width : width / tabs.size();
        for (int i = 0; i < tabs.size(); i++) {
            T tab = tabs.get(i);
            int x = getX() + i * segmentWidth;
            int tabWidth = i == tabs.size() - 1 ? getX() + width - x : segmentWidth;
            FranklyButton button = FranklyButton.builder()
                    .bounds(x, getY(), tabWidth, getHeight())
                    .message(labelMapper.apply(tab))
                    .active(!tab.equals(current))
                    .onPress(btn -> {
                        current = tab;
                        onSelect.accept(tab);
                    })
                    .build();
            button.setX(x);
            button.setY(getY());
            button.setWidth(tabWidth);
            button.setHeight(getHeight());
            buttons.add(button);
            button.extractContents(graphics, mouseX, mouseY, delta);
        }
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static final class Builder<T> {
        private int x;
        private int y;
        private int width = 120;
        private int height = 20;
        private List<T> tabs = List.of();
        private @Nullable Function<T, Component> labelMapper;
        private T current;
        private @Nullable Consumer<T> onSelect;

        public Builder<T> bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder<T> tabs(List<T> tabs) {
            this.tabs = tabs;
            return this;
        }

        public Builder<T> labelMapper(Function<T, Component> labelMapper) {
            this.labelMapper = labelMapper;
            return this;
        }

        public Builder<T> current(T current) {
            this.current = current;
            return this;
        }

        public Builder<T> onSelect(Consumer<T> onSelect) {
            this.onSelect = onSelect;
            return this;
        }

        public FranklyTabBar<T> build() {
            return new FranklyTabBar<>(x, y, width, height, tabs, labelMapper, current, onSelect);
        }
    }
}