package com.frank1o3.franklylib.client.gui.animation;

/** A resolved optional UI transform. Identity is used when no animation is selected. */
public record FranklyUiAnimation(float translateX, float translateY, float scale, float alpha) {
    public static final FranklyUiAnimation IDENTITY = new FranklyUiAnimation(0f, 0f, 1f, 1f);

    public static FranklyUiAnimation lerp(FranklyUiAnimation from, FranklyUiAnimation to, float progress) {
        return new FranklyUiAnimation(
                from.translateX + (to.translateX - from.translateX) * progress,
                from.translateY + (to.translateY - from.translateY) * progress,
                from.scale + (to.scale - from.scale) * progress,
                from.alpha + (to.alpha - from.alpha) * progress);
    }
}
