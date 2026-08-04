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
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
public class FranklyTextBox extends AbstractWidget {
    private static final int COLOR_BG = 0x54_444444;
    private static final int COLOR_BORDER = 0xFF_BBBBBB;
    private static final int COLOR_TEXT = 0xFF_FFFFFF;
    private static final int COLOR_CURSOR = 0xFF_FFFFFF;
    private final Predicate<String> filter;
    private final int maxLength;
    private final Consumer<String> onChanged;
    private final Consumer<String> onSubmit;
    private String value;
    private int cursor;
    private boolean focused;
    private int scrollOffset;

    private FranklyTextBox(int x, int y, int width, int height, String initialValue, Predicate<String> filter,
            int maxLength, Consumer<String> onChanged, Consumer<String> onSubmit) {
        super(x, y, width, height, Component.empty());
        this.value = initialValue == null ? "" : initialValue;
        this.filter = filter;
        this.maxLength = maxLength;
        this.onChanged = onChanged != null ? onChanged : s -> {
        };
        this.onSubmit = onSubmit != null ? onSubmit : s -> {
        };
        this.cursor = this.value.length();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(getX(), getY(), getX() + width, getY() + height, COLOR_BG);
        graphics.fill(getX(), getY(), getX() + width, getY() + 1, COLOR_BORDER);
        graphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, COLOR_BORDER);
        graphics.fill(getX(), getY(), getX() + 1, getY() + height, COLOR_BORDER);
        graphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, COLOR_BORDER);

        Font font = Minecraft.getInstance().font;
        int textX = getX() + 3;
        int textY = getY() + (height - font.lineHeight) / 2;
        String visible = value.substring(Math.max(0, scrollOffset));
        int availableWidth = width - 6;
        if (font.width(visible) > availableWidth) {
            while (scrollOffset < value.length() && font.width(value.substring(scrollOffset)) > availableWidth) {
                scrollOffset++;
            }
            visible = value.substring(scrollOffset);
        }
        graphics.text(font, visible, textX, textY, COLOR_TEXT, false);

        if (focused && (int) (System.currentTimeMillis() / 500) % 2 == 0) {
            int caretX = textX
                    + font.width(visible.substring(0, Math.max(0, Math.min(cursor - scrollOffset, visible.length()))));
            graphics.fill(caretX, textY, caretX + 1, textY + font.lineHeight, COLOR_CURSOR);
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        focused = true;
        cursor = value.length();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == 259) {
            if (cursor > 0) {
                value = value.substring(0, cursor - 1) + value.substring(cursor);
                cursor--;
                onChanged.accept(value);
            }
            return true;
        }
        if (key == 257) {
            onSubmit.accept(value);
            return true;
        }
        if (key == 262) {
            cursor = Math.min(value.length(), cursor + 1);
            return true;
        }
        if (key == 263) {
            cursor = Math.max(0, cursor - 1);
            return true;
        }
        char c = (char) key;
        if (c >= 32 && c <= 126) {
            String next = value.substring(0, cursor) + c + value.substring(cursor);
            if (maxLength <= 0 || next.length() <= maxLength) {
                if (filter == null || filter.test(next)) {
                    value = next;
                    cursor++;
                    onChanged.accept(value);
                }
            }
        }
        return true;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.literal(value));
    }

    protected MutableComponent createNarrationMessage() {
        return Component.literal(value);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int x;
        private int y;
        private int width = 120;
        private int height = 20;
        private String initialValue = "";
        private @Nullable Predicate<String> filter;
        private int maxLength = -1;
        private @Nullable Consumer<String> onChanged;
        private @Nullable Consumer<String> onSubmit;

        public Builder bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder initialValue(String initialValue) {
            this.initialValue = initialValue;
            return this;
        }

        public Builder filter(Predicate<String> filter) {
            this.filter = filter;
            return this;
        }

        public Builder maxLength(int maxLength) {
            this.maxLength = maxLength;
            return this;
        }

        public Builder onChanged(Consumer<String> onChanged) {
            this.onChanged = onChanged;
            return this;
        }

        public Builder onSubmit(Consumer<String> onSubmit) {
            this.onSubmit = onSubmit;
            return this;
        }

        public FranklyTextBox build() {
            return new FranklyTextBox(x, y, width, height, initialValue, filter, maxLength, onChanged, onSubmit);
        }
    }
}
