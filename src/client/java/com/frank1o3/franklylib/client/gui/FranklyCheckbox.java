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

    private FranklyCheckbox(int x, int y, int width, int height, Component label, boolean checked,
            Consumer<Boolean> onToggle) {
        super(x, y, width, height, Component.empty());
        this.label = label;
        this.checked = checked;
        this.onToggle = onToggle != null ? onToggle : value -> {
        };
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int bg = !active ? COLOR_BG_DISABLED : isHoveredOrFocused() ? COLOR_BG_HOVER : COLOR_BG;
        graphics.fill(getX(), getY(), getX() + width, getY() + height, bg);
        graphics.fill(getX(), getY(), getX() + width, getY() + 1, COLOR_BORDER);
        graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, COLOR_BORDER);
        graphics.fill(getX(), getY(), getX() + 1, getY() + height, COLOR_BORDER);
        graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, COLOR_BORDER);

        if (checked) {
            graphics.fill(getX() + 2, getY() + 2, getX() + width - 2, getY() + height - 2, 0xFF_66CC66);
        }

        Font font = Minecraft.getInstance().font;
        int textColor = active ? COLOR_TEXT : COLOR_TEXT_DISABLED;
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

        public FranklyCheckbox build() {
            return new FranklyCheckbox(x, y, width, height, label, checked, onToggle);
        }
    }
}
