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
import net.minecraft.util.Mth;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.frank1o3.franklylib.client.gui.animation.FranklyUiAnimation;
import com.frank1o3.franklylib.client.gui.animation.FranklyUiAnimations;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyle;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyles;

import java.text.DecimalFormat;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A numeric input field with optional min/max range and step increment.
 * Supports both integer and decimal values. The widget renders as a text box
 * with + and – buttons on the right.
 *
 * <p>
 * Construct via {@link #builder()}:
 *
 * <pre>{@code
 * FranklyNumberInput number = FranklyNumberInput.builder()
 *         .bounds(x, y, 120, 20)
 *         .min(0.0)
 *         .max(100.0)
 *         .step(1.0)
 *         .initialValue(50.0)
 *         .integer(true) // force integer values
 *         .formatter(v -> Component.literal(String.format("%.0f", v)))
 *         .onValueChanged(v -> {
 *         })
 *         .onValueCommitted(v -> {
 *         })
 *         .build();
 * }</pre>
 */
@Environment(EnvType.CLIENT)
public class FranklyNumberInput extends AbstractWidget {

    // -------------------------------------------------------------------------
    // Visual constants (matching FranklyTextBox)
    // -------------------------------------------------------------------------

    private static final int COLOR_BG = 0x54_444444;
    private static final int COLOR_BG_HOVER = 0x54_666666;
    private static final int COLOR_BORDER = 0xFF_BBBBBB;
    private static final int COLOR_TEXT = 0xFF_FFFFFF;
    private static final int COLOR_TEXT_DISABLED = 0xFF_666666;
    private static final int COLOR_CURSOR = 0xFF_FFFFFF;
    private static final int COLOR_BUTTON_BG = 0x66_555555;
    private static final int COLOR_BUTTON_HOVER = 0x88_777777;
    private static final int COLOR_BUTTON_DISABLED = 0x44_333333;

    private static final int BUTTON_WIDTH = 16;
    private static final int TEXT_PADDING = 4;

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    private double minValue;
    private double maxValue;
    private double step;
    private final boolean integerMode;
    private final Function<Double, Component> formatter;
    private final Consumer<Double> onValueChanged;
    private final Consumer<Double> onValueCommitted;
    private final @Nullable Identifier animation;
    private final @Nullable Identifier style;

    // -------------------------------------------------------------------------
    // Mutable state
    // -------------------------------------------------------------------------

    private double value;
    private String editingText; // null when not actively editing
    private int cursorPos;
    @SuppressWarnings("unused")
    private boolean hasUncommittedChange;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    private FranklyNumberInput(Builder b) {
        super(b.x, b.y, b.width, Math.max(b.height, 20), Component.empty());
        this.minValue = b.min;
        this.maxValue = b.max;
        this.step = Math.abs(b.step);
        this.integerMode = b.integer;
        this.formatter = b.formatter != null ? b.formatter
                : integerMode ? v -> Component.literal(new DecimalFormat("#").format(v))
                        : v -> Component.literal(new DecimalFormat("0.##").format(v));
        this.onValueChanged = b.onValueChanged != null ? b.onValueChanged : v -> {
        };
        this.onValueCommitted = b.onValueCommitted != null ? b.onValueCommitted : v -> {
        };
        this.animation = b.animation;
        this.style = b.style;

        this.value = Mth.clamp(b.initialValue, minValue, maxValue);
        this.editingText = null;
        this.cursorPos = 0;
        this.hasUncommittedChange = false;
        refreshMessage();
    }

    // =========================================================================
    // Public API
    // =========================================================================

    public double getValue() {
        return value;
    }

    public void setValue(double newValue) {
        double clamped = Mth.clamp(snapToStep(newValue), minValue, maxValue);
        if (Double.compare(this.value, clamped) != 0) {
            this.value = clamped;
            hasUncommittedChange = true;
            onValueChanged.accept(this.value);
            refreshMessage();
        }
    }

    public void setMin(double min) {
        this.minValue = Math.min(min, this.maxValue);
        setValue(this.value);
    }

    public void setMax(double max) {
        this.maxValue = Math.max(max, this.minValue);
        setValue(this.value);
    }

    public void setStep(double step) {
        this.step = Math.abs(step);
    }

    // We'll implement setStep, but for now, keep simple: step is final.
    // I'll add a note in builder about runtime step change via a separate method.

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Font font = Minecraft.getInstance().font;
        FranklyUiAnimation frame = FranklyUiAnimations.beginTransform(graphics, this, animation,
                isHoveredOrFocused(), active, getX() + width / 2f, getY() + height / 2f);

        FranklyUiStyle uiStyle = FranklyUiStyles.resolve(style,
                new FranklyUiStyle(COLOR_BG, COLOR_BG_HOVER, 0x54222222, COLOR_BORDER, COLOR_TEXT,
                        COLOR_TEXT_DISABLED, COLOR_CURSOR, TEXT_PADDING, FranklyUiStyle.BorderType.SQUARE, 0, 1));
        uiStyle.withAlpha(frame.alpha()).drawBox(graphics, getX(), getY(), width, height, isHoveredOrFocused(), active);

        // Text area (excluding buttons)
        int textLeft = getX() + uiStyle.padding();
        int textRight = getX() + width - BUTTON_WIDTH - 2;
        int textY = getY() + (height - font.lineHeight) / 2;

        String displayText = (isFocused() && editingText != null) ? editingText : getFormattedValue();
        int textColor = uiStyle.text(active);

        // Scissor to keep text within the text area
        graphics.enableScissor(textLeft, getY(), textRight, getY() + height);
        graphics.text(font, displayText, textLeft, textY, textColor, false);
        graphics.disableScissor();

        // Cursor (only when focused and editing)
        if (isFocused() && editingText != null && (int) (System.currentTimeMillis() / 500) % 2 == 0) {
            int caretX = textLeft + font.width(displayText.substring(0, Math.min(cursorPos, displayText.length())));
            graphics.fill(caretX, textY, caretX + 1, textY + font.lineHeight, uiStyle.accentColor());
        }

        // Up/down buttons
        int btnX = getX() + width - BUTTON_WIDTH;
        int btnY = getY() + 1;
        int btnHeight = height - 2;

        // Up button
        boolean hoverUp = isMouseOverButton(mouseX, mouseY, btnX, btnY, BUTTON_WIDTH, btnHeight / 2);
        boolean hoverDown = isMouseOverButton(mouseX, mouseY, btnX, btnY + btnHeight / 2, BUTTON_WIDTH, btnHeight / 2);
        int upBg = !active ? COLOR_BUTTON_DISABLED : hoverUp ? COLOR_BUTTON_HOVER : COLOR_BUTTON_BG;
        int downBg = !active ? COLOR_BUTTON_DISABLED : hoverDown ? COLOR_BUTTON_HOVER : COLOR_BUTTON_BG;

        // Draw buttons with simple rectangles (no border for simplicity)
        graphics.fill(btnX, btnY, btnX + BUTTON_WIDTH, btnY + btnHeight / 2, upBg);
        graphics.fill(btnX, btnY + btnHeight / 2, btnX + BUTTON_WIDTH, btnY + btnHeight, downBg);

        // Plus/minus symbols
        int textColorBtn = active ? 0xFF_FFFFFF : 0xFF_666666;
        String upSymbol = "+";
        String downSymbol = "-";
        int symbolYOffset = (btnHeight / 2 - font.lineHeight) / 2;
        graphics.text(font, upSymbol, btnX + (BUTTON_WIDTH - font.width(upSymbol)) / 2,
                btnY + symbolYOffset, textColorBtn, false);
        graphics.text(font, downSymbol, btnX + (BUTTON_WIDTH - font.width(downSymbol)) / 2,
                btnY + btnHeight / 2 + symbolYOffset, textColorBtn, false);
        FranklyUiAnimations.endTransform(graphics);
    }

    private boolean isMouseOverButton(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    // -------------------------------------------------------------------------
    // Mouse input
    // -------------------------------------------------------------------------

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (!active)
            return;

        int btnX = getX() + width - BUTTON_WIDTH;
        int btnY = getY() + 1;
        int btnHalf = (height - 2) / 2;
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();

        // Check up/down buttons
        if (mouseX >= btnX && mouseX < btnX + BUTTON_WIDTH) {
            if (mouseY >= btnY && mouseY < btnY + btnHalf) {
                // up
                adjustBy(+step);
                commit();
                return;
            } else if (mouseY >= btnY + btnHalf && mouseY < btnY + height - 2) {
                // down
                adjustBy(-step);
                commit();
                return;
            }
        }

        // Click in text area -> focus, set cursor
        setFocused(true);
        if (editingText == null) {
            editingText = getFormattedValue();
            cursorPos = editingText.length();
        }
        // Update cursor position based on click
        Font font = Minecraft.getInstance().font;
        int textLeft = getX() + TEXT_PADDING;
        int clickX = (int) (event.x() - textLeft);
        String display = editingText;
        int newCursor = 0;
        for (int i = 0; i <= display.length(); i++) {
            if (font.width(display.substring(0, i)) > clickX) {
                newCursor = Math.max(0, i - 1);
                break;
            }
            newCursor = i;
        }
        cursorPos = Math.min(display.length(), newCursor);
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double deltaX, double deltaY) {
        // Not used for this widget
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        // Not needed
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (!active || !isHovered())
            return false;
        adjustBy(Math.signum(vertical) * step);
        commit();
        return true;
    }

    // -------------------------------------------------------------------------
    // Keyboard input
    // -------------------------------------------------------------------------

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (!isFocused() || !active)
            return super.keyPressed(event);

        int key = event.key();

        if (key == GLFW.GLFW_KEY_UP) {
            adjustBy(step);
            commit();
            return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN) {
            adjustBy(-step);
            commit();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            commit();
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            // Cancel editing, revert to current value
            editingText = null;
            setFocused(false);
            return true;
        }

        // Handle text editing keys
        if (editingText == null) {
            editingText = getFormattedValue();
            cursorPos = editingText.length();
        }

        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (cursorPos > 0) {
                editingText = editingText.substring(0, cursorPos - 1) + editingText.substring(cursorPos);
                cursorPos--;
                updateFromEditingText();
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_DELETE) {
            if (cursorPos < editingText.length()) {
                editingText = editingText.substring(0, cursorPos) + editingText.substring(cursorPos + 1);
                updateFromEditingText();
            }
            return true;
        }
        if (key == GLFW.GLFW_KEY_LEFT) {
            cursorPos = Math.max(0, cursorPos - 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT) {
            cursorPos = Math.min(editingText.length(), cursorPos + 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_HOME) {
            cursorPos = 0;
            return true;
        }
        if (key == GLFW.GLFW_KEY_END) {
            cursorPos = editingText.length();
            return true;
        }

        // Let charTyped handle printable characters
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (!isFocused() || !active)
            return false;

        char c = (char) event.codepoint();
        if (c >= 32 && c != 127) {
            if (editingText == null) {
                editingText = getFormattedValue();
                cursorPos = editingText.length();
            }
            // Build candidate
            String candidate = editingText.substring(0, cursorPos) + c + editingText.substring(cursorPos);
            // Validate: allow digits, minus sign, decimal point
            if (isValidNumberText(candidate)) {
                editingText = candidate;
                cursorPos++;
                updateFromEditingText();
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Validation helpers
    // -------------------------------------------------------------------------

    private boolean isValidNumberText(String text) {
        if (text.isEmpty())
            return true;
        // Allow only digits, optional minus at start, one decimal point
        if (integerMode && text.contains("."))
            return false;
        if (text.startsWith("-") || text.startsWith("+")) {
            if (text.length() == 1)
                return true;
            text = text.substring(1);
        }
        int dotCount = 0;
        for (char c : text.toCharArray()) {
            if (c == '.') {
                dotCount++;
                if (dotCount > 1)
                    return false;
            } else if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    private void updateFromEditingText() {
        try {
            if (editingText == null || editingText.isEmpty())
                return;
            double parsed = Double.parseDouble(editingText);
            double snapped = snapToStep(parsed);
            double clamped = Mth.clamp(snapped, minValue, maxValue);
            if (Double.compare(clamped, value) != 0) {
                value = clamped;
                hasUncommittedChange = true;
                onValueChanged.accept(value);
                refreshMessage();
            }
        } catch (NumberFormatException ignored) {
            // ignore invalid input
        }
    }

    // -------------------------------------------------------------------------
    // Value adjustment and commit
    // -------------------------------------------------------------------------

    private void adjustBy(double delta) {
        double newVal = snapToStep(value + delta);
        double clamped = Mth.clamp(newVal, minValue, maxValue);
        if (Double.compare(clamped, value) != 0) {
            value = clamped;
            hasUncommittedChange = true;
            onValueChanged.accept(value);
            refreshMessage();
            // Cancel editing if we were editing
            editingText = null;
        }
    }

    private void commit() {
        if (editingText != null) {
            // Try to parse and apply the current edit
            try {
                double parsed = Double.parseDouble(editingText);
                double snapped = snapToStep(parsed);
                double clamped = Mth.clamp(snapped, minValue, maxValue);
                if (Double.compare(clamped, value) != 0) {
                    value = clamped;
                    hasUncommittedChange = true;
                    onValueChanged.accept(value);
                }
            } catch (NumberFormatException ignored) {
                // revert to current value
            }
            editingText = null;
        }
        hasUncommittedChange = false;
        onValueCommitted.accept(value);
        refreshMessage();
    }

    private double snapToStep(double val) {
        if (step <= 0.0)
            return val;
        double rounded = Math.round(val / step) * step;
        return rounded;
    }

    // -------------------------------------------------------------------------
    // Formatting and message
    // -------------------------------------------------------------------------

    private String getFormattedValue() {
        return formatter.apply(value).getString();
    }

    private void refreshMessage() {
        setMessage(formatter.apply(value));
    }

    // -------------------------------------------------------------------------
    // Narration
    // -------------------------------------------------------------------------

    @Override
    protected MutableComponent createNarrationMessage() {
        return Component.translatable("gui.narrate.number_input", getMessage());
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, createNarrationMessage());
    }

    // =========================================================================
    // Builder
    // =========================================================================

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("UnusedReturnValue")
    public static final class Builder {
        private int x, y, width = 120, height = 20;
        private double min = -Double.MAX_VALUE;
        private double max = Double.MAX_VALUE;
        private double step = 1.0;
        private double initialValue = 0.0;
        private boolean integer = false;
        private Function<Double, Component> formatter;
        private Consumer<Double> onValueChanged;
        private Consumer<Double> onValueCommitted;
        private @Nullable Identifier animation;
        private @Nullable Identifier style;

        private Builder() {
        }

        public Builder animation(@Nullable Identifier animation) {
            this.animation = animation;
            return this;
        }

        public Builder style(@Nullable Identifier style) {
            this.style = style;
            return this;
        }

        public Builder bounds(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder min(double min) {
            this.min = min;
            return this;
        }

        public Builder max(double max) {
            this.max = max;
            return this;
        }

        public Builder step(double step) {
            this.step = Math.abs(step);
            return this;
        }

        public Builder initialValue(double initialValue) {
            this.initialValue = initialValue;
            return this;
        }

        /** If true, only integer values are accepted (no decimal point). */
        public Builder integer(boolean integer) {
            this.integer = integer;
            return this;
        }

        public Builder formatter(Function<Double, Component> formatter) {
            this.formatter = formatter;
            return this;
        }

        public Builder onValueChanged(Consumer<Double> callback) {
            this.onValueChanged = callback;
            return this;
        }

        public Builder onValueCommitted(Consumer<Double> callback) {
            this.onValueCommitted = callback;
            return this;
        }

        public FranklyNumberInput build() {
            if (min > max) {
                throw new IllegalArgumentException("min (" + min + ") must not be greater than max (" + max + ")");
            }
            return new FranklyNumberInput(this);
        }
    }
}
