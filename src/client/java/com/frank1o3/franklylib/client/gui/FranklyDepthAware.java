package com.frank1o3.franklylib.client.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Interface for UI elements that support z-index (depth) layering.
 * Higher z-index values are rendered on top of lower z-index values
 * and receive event dispatch priority.
 */
@Environment(EnvType.CLIENT)
public interface FranklyDepthAware {

    /** Returns the effective z-index of this element. */
    int getZIndex();

    /** Sets the base z-index of this element. */
    void setZIndex(int zIndex);

    /** Helper to extract z-index from any element (defaults to 0 if not depth-aware). */
    static int getZIndex(Object element) {
        if (element instanceof FranklyDepthAware depthAware) {
            return depthAware.getZIndex();
        }
        return 0;
    }
}
