package com.frank1o3.franklylib.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyle;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyles;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Environment(EnvType.CLIENT)
public class FranklyTabBar<T> extends AbstractWidget implements FranklyDepthAware {
    private final List<T> tabs;
    private final Function<T, Component> labelMapper;
    private final Consumer<T> onSelect;
    private T current;
    private final @Nullable Identifier style;
    private int zIndex;

    private FranklyTabBar(int x, int y, int width, int height, List<T> tabs, Function<T, Component> labelMapper,
            T current, Consumer<T> onSelect, @Nullable Identifier style, int zIndex) {
        super(x, y, width, height, Component.empty());
        this.tabs = new ArrayList<>(tabs);
        this.labelMapper = labelMapper != null ? labelMapper : tab -> Component.literal(String.valueOf(tab));
        this.current = current;
        this.onSelect = onSelect != null ? onSelect : value -> {
        };
        this.style = style;
        this.zIndex = zIndex;
    }

    @Override
    public int getZIndex() {
        return zIndex;
    }

    @Override
    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
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
        int segmentWidth = tabs.isEmpty() ? width : width / tabs.size();
        Font font = Minecraft.getInstance().font;
        FranklyUiStyle uiStyle = FranklyUiStyles.resolve(style,
                new FranklyUiStyle(0x54_444444, 0x54_666666, 0x54_222222, 0, 0xFF_FFFFFF, 0xFF_666666, 0, 2,
                        FranklyUiStyle.BorderType.NONE, 0, 0));

        for (int i = 0; i < tabs.size(); i++) {
            T tab = tabs.get(i);
            int x = getX() + i * segmentWidth;
            int tabWidth = i == tabs.size() - 1 ? getX() + width - x : segmentWidth;
            boolean tabActive = !tab.equals(current);
            boolean hovered = mouseX >= x && mouseX < x + tabWidth && mouseY >= getY() && mouseY < getY() + getHeight();

            uiStyle.drawBox(graphics, x, getY(), tabWidth, getHeight(), hovered, tabActive);

            int textColor = uiStyle.text(tabActive);
            int left = x + uiStyle.padding();
            int right = x + tabWidth - uiStyle.padding();
            FranklyGuiUtils.drawFittedText(FranklyGuiUtils.Justify.CENTER, graphics, font, labelMapper.apply(tab),
                    left, getY(), right, getY() + getHeight(), textColor);

            if (hovered) {
                graphics.requestCursor(tabActive ? CursorTypes.POINTING_HAND : CursorTypes.NOT_ALLOWED);
            }
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
        private @Nullable Identifier style;
        private @Nullable Integer zIndex;

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

        public Builder<T> style(@Nullable Identifier style) {
            this.style = style;
            return this;
        }

        /** Sets the z-index depth of this tab bar. */
        public Builder<T> zIndex(int zIndex) {
            this.zIndex = zIndex;
            return this;
        }

        public FranklyTabBar<T> build() {
            int resolvedZIndex = this.zIndex != null ? this.zIndex
                    : (style != null ? FranklyUiStyles.resolve(style, FranklyUiStyle.DEFAULT).zIndex() : 0);
            return new FranklyTabBar<>(x, y, width, height, tabs, labelMapper, current, onSelect, style, resolvedZIndex);
        }
    }
}
