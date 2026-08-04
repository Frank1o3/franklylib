package com.frank1o3.franklylib.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

@Environment(EnvType.CLIENT)
public final class FranklyTooltipUtils {
    private static final int BG = 0xCC_222233;
    private static final int BORDER = 0xFF_9999AA;
    private static final int TEXT = 0xFF_FFFFFF;

    private FranklyTooltipUtils() {
    }

    public static void drawTooltip(GuiGraphicsExtractor graphics, Font font, int x, int y, Component message) {
        if (message == null || message.getString().isBlank()) {
            return;
        }
        int width = font.width(message) + 8;
        int height = font.lineHeight + 6;
        graphics.fill(x, y, x + width, y + height, BG);
        graphics.fill(x, y, x + width, y + 1, BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER);
        graphics.fill(x, y, x + 1, y + height, BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, BORDER);
        graphics.text(font, message, x + 4, y + 3, TEXT, false);
    }
}
