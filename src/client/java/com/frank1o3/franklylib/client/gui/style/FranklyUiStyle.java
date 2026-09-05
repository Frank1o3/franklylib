package com.frank1o3.franklylib.client.gui.style;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The resolved visual properties for a UI element. Values are ARGB colours.
 * This deliberately stays small: it is a shared skin, not a second layout
 * language.
 */
@Environment(EnvType.CLIENT)
public record FranklyUiStyle(int background, int hoverBackground, int disabledBackground,
        int borderColor, int textColor, int disabledTextColor, int accentColor,
        int padding, BorderType borderType, int borderRadius, int borderWidth,
        int zIndex) {

    public FranklyUiStyle(int background, int hoverBackground, int disabledBackground,
            int borderColor, int textColor, int disabledTextColor, int accentColor,
            int padding, BorderType borderType, int borderRadius, int borderWidth) {
        this(background, hoverBackground, disabledBackground, borderColor, textColor, disabledTextColor, accentColor,
                padding, borderType, borderRadius, borderWidth, 0);
    }

    public static final FranklyUiStyle DEFAULT = new FranklyUiStyle(
            0x54444444, 0x54666666, 0x54222222, 0xFFBBBBBB,
            0xFFFFFFFF, 0xFF666666, 0xFF66CC66, 2, BorderType.SQUARE, 0, 1, 0);

    public enum BorderType { NONE, SQUARE, ROUNDED }

    public int background(boolean hovered, boolean active) {
        return active ? (hovered ? hoverBackground : background) : disabledBackground;
    }

    public int text(boolean active) {
        return active ? textColor : disabledTextColor;
    }

    /** Returns this style with every colour's alpha multiplied by {@code alpha}. */
    public FranklyUiStyle withAlpha(float alpha) {
        return new FranklyUiStyle(applyAlpha(background, alpha), applyAlpha(hoverBackground, alpha),
                applyAlpha(disabledBackground, alpha), applyAlpha(borderColor, alpha), applyAlpha(textColor, alpha),
                applyAlpha(disabledTextColor, alpha), applyAlpha(accentColor, alpha), padding, borderType,
                borderRadius, borderWidth, zIndex);
    }

    /** Returns a copy of this style with the given z-index. */
    public FranklyUiStyle withZIndex(int zIndex) {
        return new FranklyUiStyle(background, hoverBackground, disabledBackground, borderColor, textColor,
                disabledTextColor, accentColor, padding, borderType, borderRadius, borderWidth, zIndex);
    }

    /** Draws a fill and optional border using only vanilla GUI primitives. */
    public void drawBox(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
            boolean hovered, boolean active) {
        drawRoundedRect(graphics, x, y, width, height, background(hovered, active),
                borderType == BorderType.ROUNDED ? borderRadius : 0);
        if (borderType != BorderType.NONE && borderWidth > 0) {
            drawBorder(graphics, x, y, width, height, borderColor,
                    borderType == BorderType.ROUNDED ? borderRadius : 0, borderWidth);
        }
    }

    public static void drawRoundedRect(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
            int color, int radius) {
        int r = Math.max(0, Math.min(radius, Math.min(width, height) / 2));
        if (r == 0) {
            graphics.fill(x, y, x + width, y + height, color);
            return;
        }
        // A scanline rasterisation gives consistent rounded corners without a texture.
        for (int row = 0; row < height; row++) {
            int inset = row < r ? cornerInset(r, row) : row >= height - r ? cornerInset(r, height - 1 - row) : 0;
            graphics.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
        }
    }

    private static void drawBorder(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
            int color, int radius, int thickness) {
        int max = Math.min(thickness, Math.min(width, height) / 2);
        for (int inset = 0; inset < max; inset++) {
            int w = width - inset * 2;
            int h = height - inset * 2;
            int r = Math.max(0, radius - inset);
            for (int row = 0; row < h; row++) {
                int corner = row < r ? cornerInset(r, row) : row >= h - r ? cornerInset(r, h - 1 - row) : 0;
                if (row < max || row >= h - max) {
                    graphics.fill(x + inset + corner, y + inset + row, x + inset + w - corner, y + inset + row + 1, color);
                } else {
                    graphics.fill(x + inset + corner, y + inset + row, x + inset + max, y + inset + row + 1, color);
                    graphics.fill(x + inset + w - max, y + inset + row, x + inset + w - corner, y + inset + row + 1, color);
                }
            }
        }
    }

    private static int cornerInset(int radius, int row) {
        double dy = radius - row - .5d;
        return Math.max(0, radius - (int) Math.ceil(Math.sqrt(radius * radius - dy * dy)));
    }

    private static int applyAlpha(int color, float alpha) {
        int base = color >>> 24;
        return (Math.round(base * Math.clamp(alpha, 0f, 1f)) << 24) | (color & 0x00FFFFFF);
    }
}
