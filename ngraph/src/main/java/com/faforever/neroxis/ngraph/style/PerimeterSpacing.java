package com.faforever.neroxis.ngraph.style;

import lombok.Data;

/**
 * Spacing, in pixels, added to each side of a label in a vertex (applies to vertices only).
 */
@Data
public class PerimeterSpacing {
    /**
     * This is the distance between the source
     * connection point of an edge and the perimeter of the source vertex in
     * pixels. This only applies to edges.
     */
    double source;
    /**
     * This is the distance between the target
     * connection of an edge and the perimeter of the target vertex in
     * pixels. This style only applies to edges.
     */
    double target;
    /**
     * This is the distance between
     * the connection point and the perimeter in pixels. When used in a vertex
     * style, this applies to all incoming edges to floating ports (edges that
     * terminate on the perimeter of the vertex). When used in an edge style,
     * this spacing applies to the source and target separately, if they
     * terminate in floating ports (on the perimeter of the vertex).
     */
    double vertex;

    public void setUniform(int value) {
        source = value;
        target = value;
        vertex = value;
    }
}
