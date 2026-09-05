package com.frank1o3.franklylib.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyle;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyles;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
public class FranklyTextBox extends AbstractWidget implements FranklyDepthAware {
    private static final int COLOR_BG = 0x54_444444;
    private static final int COLOR_BORDER = 0xFF_BBBBBB;
    private static final int COLOR_TEXT = 0xFF_FFFFFF;
    private static final int COLOR_CURSOR = 0xFF_EEEEEE;
    private final Predicate<String> filter;
    private final int maxLength;
    private final Consumer<String> onChanged;
    private final Consumer<String> onSubmit;
    private String value;
    private int cursor;
    private int scrollOffset;
    private final @Nullable Identifier style;
    private int zIndex;

    private FranklyTextBox(int x, int y, int width, int height, String initialValue, Predicate<String> filter,
            int maxLength, Consumer<String> onChanged, Consumer<String> onSubmit, @Nullable Identifier style,
            int zIndex) {
        super(x, y, width, height, Component.empty());
        this.value = initialValue == null ? "" : initialValue;
        this.filter = filter;
        this.maxLength = maxLength;
        this.onChanged = onChanged != null ? onChanged : s -> {
        };
        this.onSubmit = onSubmit != null ? onSubmit : s -> {
        };
        this.cursor = this.value.length();
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

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value == null ? "" : value;
        this.cursor = Math.min(cursor, this.value.length());
        this.scrollOffset = Math.min(scrollOffset, this.cursor);
        onChanged.accept(this.value);
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        FranklyUiStyle uiStyle = FranklyUiStyles.resolve(style,
                new FranklyUiStyle(COLOR_BG, COLOR_BG, COLOR_BG, COLOR_BORDER, COLOR_TEXT, COLOR_TEXT, COLOR_CURSOR, 3,
                        FranklyUiStyle.BorderType.SQUARE, 0, 1));
        uiStyle.drawBox(graphics, getX(), getY(), width, height, isHoveredOrFocused(), active);

        Font font = Minecraft.getInstance().font;
        int textX = getX() + uiStyle.padding();
        int textY = getY() + (height - font.lineHeight) / 2;

        if (cursor < scrollOffset) {
            scrollOffset = cursor;
        }

        String visible = value.substring(Math.max(0, Math.min(scrollOffset, value.length())));
        int availableWidth = width - uiStyle.padding() * 2;
        if (font.width(visible) > availableWidth) {
            while (scrollOffset < value.length() && font.width(value.substring(scrollOffset)) > availableWidth) {
                scrollOffset++;
            }
            visible = value.substring(Math.min(scrollOffset, value.length()));
        }

        graphics.text(font, visible, textX, textY, uiStyle.text(active), false);

        if (isFocused() && (int) (System.currentTimeMillis() / 500) % 2 == 0) {
            int caretX = textX
                    + font.width(visible.substring(0, Math.max(0, Math.min(cursor - scrollOffset, visible.length()))));
            graphics.fill(caretX, textY, caretX + 1, textY + font.lineHeight, uiStyle.accentColor());
        }
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        setFocused(true);
        Font font = Minecraft.getInstance().font;
        int clickX = (int) (event.x() - (getX() + 3));
        String visible = value.substring(Math.max(0, Math.min(scrollOffset, value.length())));
        int relCursor = 0;
        for (int i = 0; i <= visible.length(); i++) {
            if (font.width(visible.substring(0, i)) > clickX) {
                relCursor = Math.max(0, i - 1);
                break;
            }
            relCursor = i;
        }
        cursor = Math.min(value.length(), Math.max(0, scrollOffset + relCursor));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!isFocused() || !active) {
            return super.keyPressed(event);
        }

        int key = event.key();

        if (key == 259) { // Backspace
            if (cursor > 0) {
                value = value.substring(0, cursor - 1) + value.substring(cursor);
                cursor--;
                onChanged.accept(value);
            }
            return true;
        }
        if (key == 261) { // Delete
            if (cursor < value.length()) {
                value = value.substring(0, cursor) + value.substring(cursor + 1);
                onChanged.accept(value);
            }
            return true;
        }
        if (key == 257 || key == 335) { // Enter / Keypad Enter
            onSubmit.accept(value);
            return true;
        }
        if (key == 262) { // Right Arrow
            cursor = Math.min(value.length(), cursor + 1);
            return true;
        }
        if (key == 263) { // Left Arrow
            cursor = Math.max(0, cursor - 1);
            return true;
        }
        if (key == 268) { // Home
            cursor = 0;
            return true;
        }
        if (key == 269) { // End
            cursor = value.length();
            return true;
        }

        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!isFocused() || !active) {
            return false;
        }
        char c = (char) event.codepoint();
        if (c >= 32 && c != 127) {
            String next = value.substring(0, cursor) + c + value.substring(cursor);
            if (maxLength <= 0 || next.length() <= maxLength) {
                if (filter == null || filter.test(next)) {
                    value = next;
                    cursor++;
                    onChanged.accept(value);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.literal(value));
    }

    @Override
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
        private @Nullable Identifier style;

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

        private @Nullable Integer zIndex;

        public Builder style(@Nullable Identifier style) {
            this.style = style;
            return this;
        }

        /** Sets the z-index depth of this text box. */
        public Builder zIndex(int zIndex) {
            this.zIndex = zIndex;
            return this;
        }

        public FranklyTextBox build() {
            int resolvedZIndex = this.zIndex != null ? this.zIndex
                    : (style != null ? FranklyUiStyles.resolve(style, FranklyUiStyle.DEFAULT).zIndex() : 0);
            return new FranklyTextBox(x, y, width, height, initialValue, filter, maxLength, onChanged, onSubmit, style, resolvedZIndex);
        }
    }
}
