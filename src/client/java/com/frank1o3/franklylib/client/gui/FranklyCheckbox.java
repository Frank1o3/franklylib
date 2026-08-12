package com.frank1o3.franklylib.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyle;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyles;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class FranklyCheckbox extends AbstractWidget {
    private static final int COLOR_BG = 0x54_444444;
    private static final int COLOR_BG_HOVER = 0x54_666666;
    private static final int COLOR_BG_DISABLED = 0x54_222222;
    private static final int COLOR_BORDER = 0xFF_BBBBBB;
    private static final int COLOR_TEXT = 0xFF_FFFFFF;
    private static final int COLOR_TEXT_DISABLED = 0xFF_666666;

    private final Component label;
    private final Consumer<Boolean> onToggle;
    private boolean checked;
    private final @Nullable Identifier style;

    private FranklyCheckbox(int x, int y, int width, int height, Component label, boolean checked,
            Consumer<Boolean> onToggle, @Nullable Identifier style) {
        super(x, y, width, height, Component.empty());
        this.label = label;
        this.checked = checked;
        this.onToggle = onToggle != null ? onToggle : value -> {
        };
        this.style = style;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        FranklyUiStyle uiStyle = FranklyUiStyles.resolve(style,
                new FranklyUiStyle(COLOR_BG, COLOR_BG_HOVER, COLOR_BG_DISABLED, COLOR_BORDER, COLOR_TEXT,
                        COLOR_TEXT_DISABLED, 0xFF66CC66, 2, FranklyUiStyle.BorderType.SQUARE, 0, 1));
        uiStyle.drawBox(graphics, getX(), getY(), width, height, isHoveredOrFocused(), active);

        if (checked) {
            graphics.fill(getX() + uiStyle.padding(), getY() + uiStyle.padding(), getX() + width - uiStyle.padding(), getY() + height - uiStyle.padding(), uiStyle.accentColor());
        }

        Font font = Minecraft.getInstance().font;
        int textColor = uiStyle.text(active);
        graphics.text(font, label, getX() + width + 6, getY() + (height - font.lineHeight) / 2, textColor, false);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (!active) {
            return;
        }
        checked = !checked;
        onToggle.accept(checked);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 32) {
            checked = !checked;
            onToggle.accept(checked);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected MutableComponent createNarrationMessage() {
        return Component.translatable("gui.narrate.checkbox", label);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, createNarrationMessage());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int x;
        private int y;
        private int width = 14;
        private int height = 14;
        private Component label = Component.empty();
        private boolean checked;
        private @Nullable Consumer<Boolean> onToggle;
        private @Nullable Identifier style;

        public Builder bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder label(Component label) {
            this.label = label;
            return this;
        }

        public Builder checked(boolean checked) {
            this.checked = checked;
            return this;
        }

        public Builder onToggle(Consumer<Boolean> onToggle) {
            this.onToggle = onToggle;
            return this;
        }

        public Builder style(@Nullable Identifier style) {
            this.style = style;
            return this;
        }

        public FranklyCheckbox build() {
            return new FranklyCheckbox(x, y, width, height, label, checked, onToggle, style);
        }
    }
}
