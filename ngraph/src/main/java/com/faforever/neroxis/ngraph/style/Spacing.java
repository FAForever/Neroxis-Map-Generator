package com.faforever.neroxis.ngraph.style;

import lombok.Data;

/**
 * Spacing, in pixels, added to each side of a label in a vertex (applies to vertices only).
 */
@Data
public class Spacing {
    int top;
    int left;
    int bottom;
    int right;

    public void setUniform(int value) {
        top = value;
        left = value;
        bottom = value;
        right = value;
    }
}
