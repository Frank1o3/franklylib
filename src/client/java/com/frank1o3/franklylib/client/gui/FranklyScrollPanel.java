package com.frank1o3.franklylib.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyle;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyles;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * A vertically scrollable container for a fixed set of child widgets.
 *
 * <p>
 * Children are added via {@link #addChild(AbstractWidget, int)} with a fixed
 * Y offset relative to the panel's content top (not the screen). Every frame
 * the
 * panel writes each child's <em>real</em> x/y to
 * {@code panelXY + offset - scroll},
 * so vanilla hit-testing on the children keeps working unmodified. Add only the
 * panel itself to the owning screen — never its children directly — since the
 * panel is solely responsible for routing input to them.
 * </p>
 */
@Environment(EnvType.CLIENT)
public class FranklyScrollPanel extends AbstractWidget {
    private static final int COLOR_BG = 0x33_000000;
    private static final int SCROLLBAR_TRACK = 0x33_000000;
    private static final int SCROLLBAR_THUMB = 0x88_AAAAAA;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final double SCROLL_SPEED = 14.0;

    private final List<Entry> children = new ArrayList<>();
    private int contentHeight;
    private double scrollAmount;
    private @Nullable AbstractWidget focused;
    private @Nullable Identifier style;

    private FranklyScrollPanel(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
    }

    public static FranklyScrollPanel create(int x, int y, int width, int height) {
        return new FranklyScrollPanel(x, y, width, height);
    }

    /** Applies a resource-pack style to the panel background. */
    public FranklyScrollPanel style(@Nullable Identifier style) {
        this.style = style;
        return this;
    }

    public <T extends AbstractWidget> T addChild(T widget, int offsetY) {
        children.add(new Entry(widget, offsetY));
        contentHeight = Math.max(contentHeight, offsetY + widget.getHeight());
        layoutChildren();
        return widget;
    }

    public void clearChildren() {
        children.clear();
        contentHeight = 0;
        scrollAmount = 0;
        focused = null;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight - height);
    }

    private void layoutChildren() {
        scrollAmount = Mth.clamp(scrollAmount, 0, maxScroll());
        for (Entry entry : children) {
            entry.widget.setX(getX());
            entry.widget.setY(getY() + entry.offsetY - (int) scrollAmount);
        }
    }

    private boolean isVisible(AbstractWidget widget) {
        return widget.getY() + widget.getHeight() > getY() && widget.getY() < getY() + height;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        layoutChildren();
        FranklyUiStyle uiStyle = FranklyUiStyles.resolve(style,
                new FranklyUiStyle(COLOR_BG, COLOR_BG, COLOR_BG, 0, 0xFFFFFFFF, 0xFFFFFFFF,
                        SCROLLBAR_THUMB, 0, FranklyUiStyle.BorderType.NONE, 0, 0));
        uiStyle.drawBox(graphics, getX(), getY(), width, height, false, active);

        graphics.enableScissor(getX(), getY(), getX() + width, getY() + height);
        for (Entry entry : children) {
            if (isVisible(entry.widget)) {
                entry.widget.extractRenderState(graphics, mouseX, mouseY, delta);
            }
        }
        graphics.disableScissor();

        int max = maxScroll();
        if (max > 0 && contentHeight > 0) {
            int trackX = getX() + width - SCROLLBAR_WIDTH;
            int thumbHeight = Math.max(10, (int) ((float) height * height / contentHeight));
            int thumbY = getY() + (int) ((height - thumbHeight) * (scrollAmount / max));
            graphics.fill(trackX, getY(), trackX + SCROLLBAR_WIDTH, getY() + height, SCROLLBAR_TRACK);
            graphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, uiStyle.accentColor());
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return active && visible
                && mouseX >= getX() && mouseX < getX() + width
                && mouseY >= getY() && mouseY < getY() + height;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        for (Entry entry : children) {
            AbstractWidget widget = entry.widget;
            if (isVisible(widget) && widget.isMouseOver(event.x(), event.y())) {
                focused = widget;
                widget.mouseClicked(event, doubleClick);
                return;
            }
        }
        focused = null;
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        if (focused != null) {
            focused.mouseDragged(event, dragX, dragY);
        }
    }

    @Override
    public void onRelease(MouseButtonEvent event) {
        if (focused != null) {
            focused.mouseReleased(event);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (!isMouseOver(mouseX, mouseY) || maxScroll() == 0) {
            return false;
        }
        scrollAmount -= vertical * SCROLL_SPEED;
        layoutChildren();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (focused != null && focused.keyPressed(event)) {
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_PAGE_UP) {
            scrollAmount = Math.max(0, scrollAmount - height);
            layoutChildren();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_PAGE_DOWN) {
            scrollAmount = Math.min(maxScroll(), scrollAmount + height);
            layoutChildren();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        return focused != null && focused.keyReleased(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return focused != null && focused.charTyped(event);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
    }

    private record Entry(AbstractWidget widget, int offsetY) {
    }
}
