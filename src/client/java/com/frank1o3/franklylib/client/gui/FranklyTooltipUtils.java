package com.frank1o3.franklylib.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyle;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyles;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public final class FranklyTooltipUtils {
    private static final int BG = 0xCC_222233;
    private static final int BORDER = 0xFF_9999AA;
    private static final int TEXT = 0xFF_FFFFFF;

    private FranklyTooltipUtils() {
    }

    public static void drawTooltip(GuiGraphicsExtractor graphics, Font font, int x, int y, Component message) {
        drawTooltip(graphics, font, x, y, message, null);
    }

    /** Draws a tooltip using the optional resource-pack style. */
    public static void drawTooltip(GuiGraphicsExtractor graphics, Font font, int x, int y, Component message,
            @Nullable Identifier style) {
        if (message == null || message.getString().isBlank()) {
            return;
        }
        int width = font.width(message) + 8;
        int height = font.lineHeight + 6;
        FranklyUiStyle uiStyle = FranklyUiStyles.resolve(style,
                new FranklyUiStyle(BG, BG, BG, BORDER, TEXT, TEXT, TEXT, 4, FranklyUiStyle.BorderType.SQUARE, 0, 1));
        uiStyle.drawBox(graphics, x, y, width, height, false, true);
        graphics.text(font, message, x + uiStyle.padding(), y + 3, uiStyle.textColor(), false);
    }
}
