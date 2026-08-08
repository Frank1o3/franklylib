package com.frank1o3.franklylib.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * A small stateful cursor for laying out widgets in a vertical column without every
 * screen hand-rolling {@code y += ROW_HEIGHT} and hoping it doesn't drift. Not a
 * widget itself — just a plain helper you call {@link #next(int)} on while building
 * your widget list, the same spirit as {@code PaginatedContent}.
 *
 * <pre>{@code
 * FranklyLayout layout = FranklyLayout.column(x, tabY + 22);
 * addRenderableWidget(sizeSlider(x, layout.next(20), sliderWidth));
 * addRenderableWidget(offsetSlider(x, layout.next(20), sliderWidth, ...));
 * layout.gap(4); // extra breathing room before a new section
 * addRenderableWidget(spreadSlider(x, layout.next(20), sliderWidth));
 * }</pre>
 *
 * <p>
 * This intentionally does not try to compute widths or wrap into columns — it is the
 * minimal fix for the single most common layout bug (a row height that doesn't match
 * what was actually rendered), not a general layout engine. For anything that needs
 * to scroll, see {@link FranklyScrollPanel} instead.
 */
@Environment(EnvType.CLIENT)
public final class FranklyLayout {

    private final int x;
    private int y;
    private final int spacing;

    private FranklyLayout(int x, int y, int spacing) {
        this.x = x;
        this.y = y;
        this.spacing = spacing;
    }

    /** Starts a column at {@code (x, y)} with the default 4px spacing between rows. */
    public static FranklyLayout column(int x, int y) {
        return new FranklyLayout(x, y, 4);
    }

    /** Starts a column at {@code (x, y)} with a custom spacing between rows. */
    public static FranklyLayout column(int x, int y, int spacing) {
        return new FranklyLayout(x, y, spacing);
    }

    /** The fixed left edge this layout was started with. */
    public int x() {
        return x;
    }

    /** The next unclaimed y position, without advancing the cursor. */
    public int peek() {
        return y;
    }

    /**
     * Returns the y position for a row of {@code rowHeight} pixels, then advances the
     * cursor by {@code rowHeight + spacing} for the next call.
     */
    public int next(int rowHeight) {
        int current = y;
        y += rowHeight + spacing;
        return current;
    }

    /** Advances the cursor by an extra gap without producing a row — for section breaks. */
    public FranklyLayout gap(int extra) {
        y += extra;
        return this;
    }
}