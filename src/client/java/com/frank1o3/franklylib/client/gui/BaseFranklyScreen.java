package com.frank1o3.franklylib.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyle;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyles;
import com.frank1o3.franklylib.client.mixin.accessors.ScreenAccessor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Common chrome shared by every Proportionality screen: a dimmed background
 * overlay, a bordered panel centred on screen, and a title drawn at its top.
 *
 * <p>
 * {@link frank1o3.statscale.client.gui.screen.ScaleScreen} and the operator-only
 * scale-management screen both extend this so the panel look stays identical
 * without copy-pasting the fill/border/title code between them.
 */
@Environment(EnvType.CLIENT)
public abstract class BaseFranklyScreen extends Screen {

    private static final int OVERLAY_COLOR = 0x88_000000;
    private static final int PANEL_COLOR = 0xCC_1A1A2E;
    private static final int BORDER_COLOR = 0xFF_3A3A5E;
    private static final int TITLE_COLOR = 0xFF_FFFFFF;

    protected final @Nullable Screen parent;
    protected final int panelWidth;
    protected final int panelHeight;
    private @Nullable Identifier style;

    protected BaseFranklyScreen(Component title, @Nullable Screen parent, int panelWidth, int panelHeight) {
        super(title);
        this.parent = parent;
        this.panelWidth = panelWidth;
        this.panelHeight = panelHeight;
    }

    /** Applies a resource-pack style to this screen's panel. Call from the subclass constructor. */
    protected final void setUiStyle(@Nullable Identifier style) {
        this.style = style;
    }

    /**
     * Left edge of the centred panel, in screen pixels. Valid only after init().
     */
    protected int panelX() {
        return width / 2 - panelWidth / 2;
    }

    /** Top edge of the centred panel, in screen pixels. Valid only after init(). */
    protected int panelY() {
        return height / 2 - panelHeight / 2;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        FranklyUiStyle uiStyle = FranklyUiStyles.resolve(style,
                new FranklyUiStyle(PANEL_COLOR, PANEL_COLOR, PANEL_COLOR, BORDER_COLOR, TITLE_COLOR, TITLE_COLOR,
                        TITLE_COLOR, 8, FranklyUiStyle.BorderType.SQUARE, 0, 1));
        graphics.fill(0, 0, width, height, OVERLAY_COLOR);

        int px = panelX();
        int py = panelY();
        uiStyle.drawBox(graphics, px, py, panelWidth, panelHeight, false, true);

        Component title = getTitle();
        int titleY = py + uiStyle.padding();
        graphics.text(font, title, width / 2 - font.width(title) / 2, titleY, uiStyle.textColor(), false);

        // Subclasses can draw extra content (previews, labels) before widgets.
        renderPanelContent(graphics, px, py, mouseX, mouseY, delta);

        renderWidgetsWithZIndex(graphics, mouseX, mouseY, delta);
    }

    /**
     * Renders child widgets in ascending z-index order so elements with higher
     * z-index (such as expanded dropdowns) are drawn in front.
     */
    protected void renderWidgetsWithZIndex(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        List<Renderable> renderables;
        try {
            renderables = ((ScreenAccessor) this).getRenderables();
        } catch (Throwable t) {
            super.extractRenderState(graphics, mouseX, mouseY, delta);
            return;
        }

        if (renderables == null || renderables.isEmpty()) {
            return;
        }

        List<Renderable> sorted = new ArrayList<>(renderables);
        sorted.sort(Comparator.comparingInt(FranklyDepthAware::getZIndex));
        for (Renderable renderable : sorted) {
            renderable.extractRenderState(graphics, mouseX, mouseY, delta);
        }
    }

    @Override
    public List<? extends GuiEventListener> children() {
        List<? extends GuiEventListener> baseChildren = super.children();
        if (baseChildren == null || baseChildren.size() <= 1) {
            return baseChildren;
        }
        // Event dispatch tests front-to-back: elements with highest z-index receive clicks first
        List<GuiEventListener> sorted = new ArrayList<>(baseChildren);
        sorted.sort((a, b) -> Integer.compare(FranklyDepthAware.getZIndex(b), FranklyDepthAware.getZIndex(a)));
        return sorted;
    }

    /**
     * Hook for subclass-specific content drawn inside the panel, after the
     * background/border/title but before child widgets. Default is a no-op.
     */
    protected void renderPanelContent(GuiGraphicsExtractor graphics, int panelX, int panelY,
            int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(parent);
    }
}
